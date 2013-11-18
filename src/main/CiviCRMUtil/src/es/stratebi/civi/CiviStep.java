package es.stratebi.civi;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import es.stratebi.civi.util.FieldAttrs;

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

public abstract class CiviStep extends BaseStep implements StepInterface {
    protected Object civiData;
    protected Object civiMeta;
    static int row = 0;

    protected static Class<?> PKG = CiviStep.class; // for i18n purposes

    public CiviStep(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    /**
     * Este metodo es el encargado de procesar las filas que se envian hacia el paso siguiente.
     * 
     * Note que como lo que realizamos son varias llamadas llamadas a la API REST de CIVI se
     * implementa a traves del metodo readOneRow
     */
    public abstract boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException;

    /**
     * Leemos solo una fila de la lista y la devolvemos para que sea enviada hacia el paso siguiente
     * Deberiamos hacer las conversiones de tipo aqui, pero debo analizar esta parte, de momento
     * todo es tratado como String
     * @throws KettleException 
     */
    
    protected abstract Object[] readOneRow() throws KettleException;
    
    protected Object getObjectValue(String field, String object) {
        try {
            if (object == null || object.equals("")) {
                return null;
            }

            FieldAttrs cf = ((CiviMeta) civiMeta).getFields().get(field);

            int metaType = ((CiviMeta) civiMeta).getMetaInterfaceType(cf.getfType());

            switch (metaType) {
            case ValueMetaInterface.TYPE_INTEGER:
                return Long.parseLong(object);
            case ValueMetaInterface.TYPE_STRING:
                return object.toString();
            case ValueMetaInterface.TYPE_NUMBER:
                return Double.parseDouble(object);
            case ValueMetaInterface.TYPE_DATE:
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                return formatter.parse(object);
            case ValueMetaInterface.TYPE_BIGNUMBER:
                return new BigDecimal(object.toString());
            case ValueMetaInterface.TYPE_BOOLEAN:
                return Boolean.parseBoolean(object);
            case ValueMetaInterface.TYPE_BINARY:
                throw new KettleValueException(toString() + " : I don't know how to convert binary values to integers.");
            case ValueMetaInterface.TYPE_SERIALIZABLE:
                throw new KettleValueException(toString()
                        + " : I don't know how to convert serializable values to integers.");
            default:
                throw new KettleValueException(toString() + " : Unknown type " + metaType + " specified.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // /throw new
            // Exception("Unexpected conversion error while converting value ["+toString()+"] to an Integer",
            // e);
        }
        return null;
    }

    /*
     * Este metodo es el primero en se llamado cuando se inicia el paso, en caso de retornar false e
     * interrumpe el mismo y finaliza la transformacion.
     * 
     * Tener en cuenta que los valores de StepMetaInterface son los que se obtienen de leer los
     * datos desde el repositorio donde esta la transformacion guardada. Por tanto antes deben
     * haberse inicializado StepMetaInterface y StepDataInterface. La excepcion es cuando se esta
     * haciendo una previsualizacion la cual no lee estos del repositorio y pasa directamente a
     * processRow tomando los valores que tiene en memoria. De esta forma la secuencia seria la
     * siguiente:
     */
    // ..... Previusualizando: .......... Ejecutando:
    // ..... --- No se ejecuta ---- ..... CiviInputMeta.loadXml()
    // ..... CiviInput.init(...) ........ CiviInput.init(...)
    // ..... CiviInput.processRow(...) .. CiviInput.processRow(...)
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        civiMeta = smi;
        civiData = sdi;

        /*
         * Aqui garantizamos seguir el procesamiento de las filas extraidas de CiviCRM, si fallara
         * algo deberiamos retornar false y kettle detendria el paso y la transformacion
         */
        boolean passed = true;
        if (super.init(smi, sdi)) {
            if (Const.isEmpty(environmentSubstitute(((CiviMeta) civiMeta).getCiviCrmApiKey()))) {
                logError(BaseMessages.getString(PKG, "CiviCrmStep.Error.EmptyApiKey"));
                passed = false;
            }
            if (passed && Const.isEmpty(environmentSubstitute(((CiviMeta) civiMeta).getCiviCrmSiteKey()))) {
                logError(BaseMessages.getString(PKG, "CiviCrmStep.Error.EmptySiteKey"));
                passed = false;
            }
            if (passed && Const.isEmpty(environmentSubstitute(((CiviMeta) civiMeta).getCiviCrmRestUrl()))) {
                logError(BaseMessages.getString(PKG, "CiviCrmStep.Error.EmptyRestUrl"));
                passed = false;
            }
            if (passed && Const.isEmpty(environmentSubstitute(((CiviMeta) civiMeta).getCiviCrmEntity()))) {
                logError(BaseMessages.getString(PKG, "CiviCrmStep.Error.EmptyEntity"));
                passed = false;
            }
        }

        return passed;
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        super.dispose(smi, sdi);
    }
}
