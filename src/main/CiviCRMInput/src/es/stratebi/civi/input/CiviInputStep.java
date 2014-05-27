package es.stratebi.civi.input;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import es.stratebi.civi.CiviStep;
import es.stratebi.civi.util.JSONString;
import es.stratebi.civi.util.RestUtil;

/*
 * 
 * Esta es la clase principal y es la que lleva la logica del plugin, tiene un metodo init llamado
 * al inicio para definir los valores de inicializacion del paso y luego internamente en la clase
 * BaseStep se hacen llamadas repetitivas al metodo processRow hasta que no tengan mas filas que
 * procesar. En el proyecto faltan aun las validaciones como la del chequeo de conexion y la
 * definicion y conversion de los tipos de datos para los campos que devuelve CiviCRM, que es el
 * proximo paso antes de tomar la salida
 */

public class CiviInputStep extends CiviStep implements StepInterface {
  private RestUtil crUtil;
  private boolean isSending = false;
  private boolean hasPreviousStep = false;
  private Object[] previousData;

  public CiviInputStep(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
    super(s, stepDataInterface, c, t, dis);
  }

  /**
   * Este metodo es el encargado de procesar las filas que se envian hacia el
   * paso siguiente.
   * 
   * Note que como lo que realizamos son varias llamadas llamadas a la API REST
   * de CIVI se implementa a traves del metodo readOneRow
   */
  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    civiMeta = (CiviInputMeta) smi;
    civiData = (CiviInputData) sdi;

    if (first) {
      if (((CiviInputMeta) civiMeta).hasPreviousStep() && !this.isSending) {
        this.previousData = getRow();
        crUtil.setOffset(0);
        if (this.previousData == null) {
          setOutputDone();
          return false;
        }
        isSending = true;
      }

      /*
       * Si existe un paso anterior entonces tomamos los metadatos en la
       * variable y ponemos el campo hasPrevious en true para procesar los datos
       * antes de llamar al API de CiviCRM
       */
      if (getInputRowMeta() != null) {
        ((CiviInputData) civiData).previousRowMeta = (RowMetaInterface) getInputRowMeta().clone();
        this.hasPreviousStep = true;
      }

      /*
       * Inicializamos el objeto para enviar los datos hacia el otro paso, no
       * tomamos los datos del metadata que le precede
       */
      ((CiviInputData) civiData).outputRowMeta = new RowMeta();

      /*
       * Aqui se obtiene un listado de los campos y sus tipos de datos para sus
       * conversiones. El truco radica en que quien procesa aqui es la clase que
       * definimos para implementar la intefaz StepDataInterface, o sea esta
       * clase es actualizada con los valores que hemos definido en el dialogo y
       * han sido almacenados en la clase que implementa StepMetaInterface en
       * nuestro caso es CiviInputMeta.
       */
      ((CiviInputMeta) civiMeta).getFields(((CiviInputData) civiData).outputRowMeta, getStepname(), null, null, this);
      // stores the indices where to look for the key fields in the input rows
      ((CiviInputData) civiData).keyFields = new HashMap<String, String>();
      List<ValueMetaInterface> metaList = ((CiviInputData) civiData).outputRowMeta.getValueMetaList();
      ((CiviInputData) civiData).conversionMeta = new ValueMetaInterface[metaList.size()];
      int i = 0;
      for (ValueMetaInterface metaInterface : metaList) {
        ValueMetaInterface returnMeta = metaInterface;

        ValueMetaInterface conversionMeta = returnMeta.clone();
        ((CiviInputData) civiData).conversionMeta[i] = conversionMeta;
        i++;
      }
    }

    /*
     * Buscar los datos del paso previo si existese y se han terminado de
     * procesar los datos obtenidos de la llamada realizada para de la fila
     * obtenida del paso anterior y generar la fila que enviamos hacia el paso
     * siguiente, si no hay mas datos entonces finalizamos el paso y retornamos
     * false para que siga la transformacion
     */

    Object[] outputRowData = null;
    if (this.hasPreviousStep) {
      if (!first) {
        if (!this.isSending) {
          this.previousData = getRow();
          crUtil.setOffset(0);
          if (this.previousData == null) {
            setOutputDone();
            return false;
          }
          // isSending = true;
        }
      }
      String[] fieldNames = getInputRowMeta().getFieldNames();
      outputRowData = readOneRow(fieldNames, this.previousData);

      isSending = (outputRowData != null);
    } else {
      outputRowData = readOneRow();
      if (outputRowData == null) {
        setOutputDone(); // Finalizamos pues ya no hay mas filas para procesar
        return false;
      }
    }

    if (first)
      first = false;

    if (outputRowData != null) {
      // Aqui convertimos a la cadena parte del JSON con el valor del campo al
      // tipo que le pertenece
      int i = 0;
      List<ValueMetaInterface> metaList = ((CiviInputData) civiData).outputRowMeta.getValueMetaList();
      for (String outputField : ((CiviInputMeta) civiMeta).getCiviCrmKeyList()) {
        try {
          Object value = getObjectValue(outputField, (String) outputRowData[i]);
          outputRowData[i] = metaList.get(i).convertData(((CiviInputData) civiData).conversionMeta[i], value);
        } catch (Exception e) {
          e.printStackTrace();
        }
        i++;
      }

      // Add the constant data to the end of the row.
      RowMetaInterface rowMeta = null;
      Object[] rowData = null;
      if (hasPreviousStep) {
        rowMeta = ((CiviInputData) civiData).previousRowMeta.clone();
        rowMeta.mergeRowMeta(((CiviInputData) civiData).outputRowMeta);
        rowData = RowDataUtil.addRowData(previousData, getInputRowMeta().size(), outputRowData);
      } else {
        rowMeta = ((CiviInputData) civiData).outputRowMeta;
        rowData = outputRowData;
      }

      putRow(rowMeta, rowData);

      // Enviando la salida hacia el paso que sigue.
      // putRow(((CiviInputData) civiData).outputRowMeta, outputRowData);

      if (checkFeedback(getLinesInput()))
        logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Msg.LinesReaded", Long.toString(getLinesRead())));
    }
    return true;
  }

  /**
   * Leemos solo una fila de la lista y la devolvemos para que sea enviada hacia
   * el paso siguiente Deberiamos hacer las conversiones de tipo aqui, pero debo
   * analizar esta parte, de momento todo es tratado como String
   * 
   * @param inData
   * @param fieldNames
   */
  protected Object[] readOneRow(String[] fieldNames, Object[] inData) throws KettleException {

    ArrayList<String> outputRow = new ArrayList<String>();
    /*
     * Estos es para iterar y buscar nuevos datos de CIVICRM, la idea es
     * deopsitar el resultado de cada llamada en un arreglo y devolver siempre
     * el primero y luego eliminarlo de la lista asi garantizamos que no estemos
     * leyendo duplicados y no colapse el sistema por falta de memoria.
     */

    if (((CiviInputData) civiData).jsonBuffer.size() == 0) {
      /*
       * Definimos los valores a establecer en los filtros y lo pasamos para que
       * sean aplicados a la llamada devolviendo lo que necesitamos
       */
      String options = "";
      int index = 0;
      boolean isFilterField = false;
      for (String filterField : ((CiviInputMeta) civiMeta).getCiviCrmFilterList()) {
        isFilterField = false;
        String oValue = ((CiviInputMeta) civiMeta).getCiviCrmFilterOperator().get(index);
        String filterFieldValue = ((CiviInputMeta) civiMeta).getCiviCrmFilterMap().get(filterField);
        if (oValue != null && !oValue.equals("") && filterFieldValue != null && !filterFieldValue.equals("")) {
          oValue = oValue.trim();
          for (int i = 0; i < fieldNames.length; i++) {
            // Buscando el campo dentro del flujo de campos de entrada al paso
            if (fieldNames[i].equals(filterFieldValue)) {
              try {
                options = options + "&" + filterField + (oValue.equals("=") ? "=" : "[" + (oValue.toLowerCase().contains("like") ? URLEncoder.encode((String) oValue, "UTF-8") : oValue) + "]=")
                    + (oValue.toLowerCase().contains("like") ? URLEncoder.encode((String) inData[i], "UTF-8") : inData[i]);
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
              }
              isFilterField = true;
              break;
            }
          }
          if (!isFilterField) {
            // El usuario introdujo un valor que no es un nombre de campo
            options = options + "&" + filterField + oValue + filterFieldValue;
          }
        }
      }

      String fields = "";
      for (String kField : ((CiviInputMeta) civiMeta).getCiviCrmKeyList()) {
        fields = fields + (fields.equals("") ? "" : ",") + kField;
      }

      if (!fields.equals("")) {
        options = options + "return=" + fields;
      }

      crUtil.setLimit(((CiviInputMeta) civiMeta).getCiviCrmPageSize());
      crUtil.setDebug(((CiviInputMeta) civiMeta).getCiviCrmDebugMode());
      crUtil.setOptions(options);

      /*
       * Deberiamos tener un contador pero no se la cantidad total de elementos
       * a retornar por lo que siempre se hace una llamada de mas
       */
      try {
        if ((first || !isSending) && ((CiviInputMeta) civiMeta).getCiviCrmPageSize() == 0) {
          // Si es la primera y no hay limites en el tamaño del resultado
          // obtener todos
          ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
          if (log.isRowLevel()) {
            logRowlevel("Call no limit url\n\t\t" + crUtil.getCallUrl());
          }
        } else if (((CiviInputMeta) civiMeta).getCiviCrmPageSize() != 0) {
          // Si hay limites en el tamaño del resultado obtener todos
          ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
          if (log.isRowLevel()) {
            logRowlevel("Call page url\n\t\t" + crUtil.getCallUrl());
          }
        }
        // Some basic logging
        first = false;
      } catch (Exception e) {
        logError("Error \n" + BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
        throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Error.InputData"), new Exception(BaseMessages.getString(PKG,
            "CiviCrmStep.Error.ErrorTitle")));
      }

      /*
       * Si luego de esta llamada no hay datos entonces retornamos un null para
       * indicar que no hay mas datos
       */
      if (((CiviInputData) civiData).jsonBuffer.size() == 0)
        return null;
    }

    Object jsonValue = ((JSONString) ((CiviInputData) civiData).jsonBuffer.get(0)).jsonObject;
    String resultField = environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmResultField());
    String fieldValue = "";
    for (String outputField : ((CiviInputMeta) civiMeta).getCiviCrmKeyList()) {
      fieldValue = "";
      try {
        if (jsonValue.getClass().getSimpleName().equals("JSONObject"))
          fieldValue = ((JSONObject) jsonValue).get(outputField).toString();
        else
          fieldValue = jsonValue.toString();

      } catch (Exception e) {
      } finally {
        outputRow.add(fieldValue);
      }
    }
    ((CiviInputData) civiData).jsonBuffer.remove(0);
    if (resultField != null && !resultField.equals("")) {
      if (log.isRowLevel()) {
        logRowlevel("JSON: " + jsonValue);
      }
      outputRow.add(jsonValue.toString());
    }
    return outputRow.toArray();
  }

  /**
   * Leemos solo una fila de la lista y la devolvemos para que sea enviada hacia
   * el paso siguiente Deberiamos hacer las conversiones de tipo aqui, pero debo
   * analizar esta parte, de momento todo es tratado como String
   */
  protected Object[] readOneRow() throws KettleException {

    ArrayList<String> outputRow = new ArrayList<String>();
    /*
     * Estos es para iterar y buscar nuevos datos de CIVICRM, la idea es
     * depositar el resultado de cada llamada en un arreglo y devolver siempre
     * el primero y luego eliminarlo de la lista asi garantizamos que no estemos
     * leyendo duplicados y no colapse el sistema por falta de memoria.
     */

    if (((CiviInputData) civiData).jsonBuffer.size() == 0) {
      // Si no hay limites en el tamaño del resultado y se llego al final
      // entonces retornar null
      if (!first && ((CiviInputMeta) civiMeta).getCiviCrmPageSize() == 0) {
        return null;
      }

      /*
       * Definimos los valores a establecer en los filtros y lo pasamos para que
       * sean aplicados a la llamada devolviendo lo que necesitamos
       */

      String options = "";
      int index = 0;
      for (String kField : ((CiviInputMeta) civiMeta).getCiviCrmFilterList()) {
        String oValue = ((CiviInputMeta) civiMeta).getCiviCrmFilterMap().get(kField);
        if (oValue != null && !oValue.equals("")) {
          String op = ((CiviInputMeta) civiMeta).getCiviCrmFilterOperator().get(index).trim();
          try {
            options = options + "&" + kField + ((op.equals("") || op.trim().equals("=")) ? "=" : "[" + (op.toLowerCase().contains("like") ? URLEncoder.encode((String) op, "UTF-8") : op)  + "]=")
                + (op.toLowerCase().contains("like") ? URLEncoder.encode((String) oValue, "UTF-8") : oValue);
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
        }
        index++;
      }

      String fields = "";
      for (String kField : ((CiviInputMeta) civiMeta).getCiviCrmKeyList()) {
        fields = fields + (fields.equals("") ? "" : ",") + kField;
      }

      if (!fields.equals("")) {
        options = options + "&return=" + fields;
      }

      crUtil.setLimit(((CiviInputMeta) civiMeta).getCiviCrmPageSize());
      crUtil.setDebug(((CiviInputMeta) civiMeta).getCiviCrmDebugMode());
      crUtil.setOptions(options);

      /*
       * Deberiamos tener un contador pero no se la cantidad total de elementos
       * a retornar por lo que siempre se hace una llamada de mas
       */
      try {
        // if (first && ((CiviInputMeta) civiMeta).getCiviCrmPageSize() == 0) {
        // // Si es la primera y no hay limites en el tamaño del resultado
        // // obtener todos
        // ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
        // if (log.isRowLevel()) {
        // logRowlevel("Call no limit url\n\t\t" + crUtil.getCallUrl());
        // }
        // } else if (((CiviInputMeta) civiMeta).getCiviCrmPageSize() != 0) {
        // Si hay limites en el tamaño del resultado obtener todos
        ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
        if (log.isRowLevel()) {
          logRowlevel("Call page url\n\t\t" + crUtil.getCallUrl());
        }
        // }
        // Some basic logging
        // first = false;
      } catch (Exception e) {
        logError("Error \n" + BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError") + e.toString());
        throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError") + e.toString(),
        // BaseMessages.getString(PKG, "CiviCrmStep.Error.InputData"),
            new Exception(BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle")));
      }

      /*
       * Si luego de esta llamada no hay datos entonces retornamos un null para
       * indicar que no hay mas datos
       */
      if (((CiviInputData) civiData).jsonBuffer.size() == 0)
        return null;
    }

    Object jsonValue = ((JSONString) ((CiviInputData) civiData).jsonBuffer.get(0)).jsonObject;
    String resultField = environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmResultField());
    String fieldValue = "";
    for (String outputField : ((CiviInputMeta) civiMeta).getCiviCrmKeyList()) {
      fieldValue = "";
      try {
        if (jsonValue.getClass().getSimpleName().equals("JSONObject"))
          fieldValue = ((JSONObject) jsonValue).get(outputField).toString();
        else
          fieldValue = jsonValue.toString();
      } catch (Exception e) {
      } finally {
        outputRow.add(fieldValue);
      }
    }
    ((CiviInputData) civiData).jsonBuffer.remove(0);
    if (resultField != null && !resultField.equals("")) {
      if (log.isRowLevel()) {
        logRowlevel("JSON: " + jsonValue);
      }
      outputRow.add(jsonValue.toString());
    }
    return outputRow.toArray();
  }

  /*
   * Este metodo es el primero en ser llamado cuando se inicia el paso, en caso
   * de retornar false e interrumpe el mismo y finaliza la transformacion.
   * 
   * Tener en cuenta que los valores de StepMetaInterface son los que se
   * obtienen de leer los datos desde el repositorio donde esta la
   * transformacion guardada. Por tanto antes deben haberse inicializado
   * StepMetaInterface y StepDataInterface. La excepcion es cuando se esta
   * haciendo una previsualizacion la cual no lee estos del repositorio y pasa
   * directamente a processRow tomando los valores que tiene en memoria. De esta
   * forma la secuencia seria la siguiente:
   */
  // ..... Previusualizando: .......... Ejecutando:
  // ..... --- No se ejecuta ---- ..... CiviInputMeta.loadXml()
  // ..... CiviInput.init(...) ........ CiviInput.init(...)
  // ..... CiviInput.processRow(...) .. CiviInput.processRow(...)
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
    super.init(smi, sdi);
    /*
     * Aqui garantizamos seguir el procesamiento de las filas extraidas de
     * CiviCRM, si fallara algo deberiamos retornar false y kettle detendria el
     * paso y la transformacion
     */
    boolean passed = super.init(smi, sdi);
    if (passed) {
      crUtil = new RestUtil(environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmRestUrl()),
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmApiKey()),
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmSiteKey()),
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmEntity()));
    }
    return passed;
  }

  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    super.dispose(smi, sdi);
  }
}
