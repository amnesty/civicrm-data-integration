package es.stratebi.civi.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
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
        // first = false;

        // Inicializamos el objeto para enviar los datos hacia el otro paso
        ((CiviInputData) civiData).outputRowMeta = new RowMeta();

        /*
         * Aqui se obtiene un listado de los campos y sus tipos de datos para sus conversiones.
         * El truco radica en que quien procesa aqui es la clase que definimos para implementar
         * la intefaz StepDataInterface, o sea esta clase es actualizada con los valores que
         * hemos definido en el dialogo y han sido almacenados en la clase que implementa
         * StepMetaInterface en nuestro caso es CiviInputMeta.
         */
        ((CiviInputMeta) civiMeta).getFields(((CiviInputData) civiData).outputRowMeta, getStepname(), null, null, this);

        // stores the indices where to look for the key fields in the input rows
        ((CiviInputData) civiData).keyFields = new HashMap<String, String>();
        ((CiviInputData) civiData).conversionMeta = new ValueMetaInterface[((CiviInputMeta) civiMeta).getFields().values().size()];
        List<ValueMetaInterface> metaList = ((CiviInputData) civiData).outputRowMeta.getValueMetaList();
        int i = 0;
        for (ValueMetaInterface metaInterface : metaList) {
            ValueMetaInterface returnMeta = metaInterface;

            ValueMetaInterface conversionMeta = returnMeta.clone();
            ((CiviInputData) civiData).conversionMeta[i] = conversionMeta;
            i++;
        }

        /**
         * Aqui deberiamos definir las conversiones dinamicamente sobre los tipos pero no lo he
         * entendido bien. Para eso definimos en util La clase CiviFieldType para mapear los
         * tipos de datos de CIVICRM contra los de Kettle y usando el API de reflection de JAVA
         * se asigna. Actualmente los tengo todos como STRING
         */
    }

    /*
     * Buscar los datos de la fila que enviamos hacia el paso siguiente, si no hay mas datos
     * entonces finalizamos el paso y retornamos false para que siga la transformaci�n
     */
    Object[] outputRowData = readOneRow();
    if (outputRowData == null) {
        setOutputDone(); // Finalizamos pues ya no hay mas filas
        return false; // para procesar.
    }

    // Aqui convertimos a la cadena parte del JSON con el valor del campo al tipo que le pertenece
    int i = 0;
    List<ValueMetaInterface> metaList = ((CiviInputData) civiData).outputRowMeta.getValueMetaList();
    for (String outputField : ((CiviInputMeta) civiMeta).getKeyList()) {
        try {
            Object value = getObjectValue(outputField, (String) outputRowData[i]);
            outputRowData[i] = metaList.get(i).convertData(((CiviInputData) civiData).conversionMeta[i], value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        i++;
    }
    // Enviando la salida hacia el paso que sigue.
    putRow(((CiviInputData) civiData).outputRowMeta, outputRowData);

    /*
     * No estoy haciendo los logs aun, debo enviar esos datos mas adelante. Debo aclara esta
     * parte
     */
    // if (checkFeedback(getLinesInput())) logBasic(BaseMessages.getString(PKG, "FixedInput.Log.LineNumber", Long.toString(getLinesInput()))); //$NON-NLS-1$

    return true;
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
     * deopsitar el resultado de cada llamada en un array y devolver siempre
     * el primero y luego eliminarlo de la lista asi garantizamos que no estemos
     * leyendo duplicados y no colapse el sistema por falta de memoria.
     */

    if (((CiviInputData) civiData).jsonBuffer.size() == 0) {
      /*
       * Definimos los valores a establecer en los filtros y lo pasamos para que
       * sean aplicados a la llamada devolviendo lo que necesitamos
       */
      String options = "";
      for (String kField : ((CiviInputMeta) civiMeta).getFilterMap().keySet()) {
        String oValue = ((CiviInputMeta) civiMeta).getFilterMap().get(kField);
        if (oValue != null && !oValue.equals(""))
          options = options + "&" + kField + "=" + oValue;
      }
      crUtil.setLimit(((CiviInputMeta) civiMeta).getCiviCrmPageSize());
      crUtil.setOptions(options);

      /*
       * Deberiamos tener un contador pero no se la cantidad total de elementos
       * a retornar por lo que siempre se hace una llamada de mas
       */
      try {
        if (first && ((CiviInputMeta) civiMeta).getCiviCrmPageSize() == 0) {
          // Si es la primera y no hay limites en el tamaño del resultado
          // obtener todos
          ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
        } else if (((CiviInputMeta) civiMeta).getCiviCrmPageSize() != 0) {
          // Si hay limites en el tamaño del resultado obtener todos
          ((CiviInputData) civiData).jsonBuffer = crUtil.getNextValues();
        }
        first = false;
      } catch (Exception e) {
        logError("Error \n" + BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
        throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Error.InputData"), new Exception(BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle")));
      }

      /*
       * Si luego de esta llamada no hay datos entonces retornamos un null para
       * indicar que no hay mas datos
       */
      if (((CiviInputData) civiData).jsonBuffer.size() == 0)
        return null;
    }

    JSONObject jsonValue = ((JSONString) ((CiviInputData) civiData).jsonBuffer.get(0)).jsonObject;
    String fieldValue = "";
    for (String outputField : ((CiviInputMeta) civiMeta).getKeyList()) {
      fieldValue = "";
      try {
        fieldValue = jsonValue.get(outputField).toString();
      } catch (Exception e) {
      } finally {
        outputRow.add(fieldValue);
      }
    }
    ((CiviInputData) civiData).jsonBuffer.remove(0);
    return outputRow.toArray();
  }


  /*
   * Este metodo es el priero en se llamado cuando se inicia el paso, en caso de
   * retornar false e interrumpe el mismo y finaliza la transformacion.
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
      crUtil = new RestUtil(
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmRestUrl()), 
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmApiKey()),
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmSiteKey()), 
          environmentSubstitute(((CiviInputMeta) civiMeta).getCiviCrmEntity())
          );
    }
    return passed;
  }

  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    super.dispose(smi, sdi);
  }
}
