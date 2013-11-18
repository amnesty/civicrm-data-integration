package es.stratebi.civi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import es.stratebi.civi.util.JSONString;

/*
 * 
 * 
 * Esta clase es la que mantiene los datos que se van a enviar hacia el paso que sigue. El truco que
 * hemos usado es crear una lista con todos aquellos valores que devueltos por el API de CIVICRM
 * para ir eliminandolos en cada llamado del m�todo readOneRow llamado por processRow.
 */
public class CiviData extends BaseStepData implements StepDataInterface {

    public RowMetaInterface outputRowMeta;

    public HashMap<String, String> keyFields;
    public HashMap<String, String> outputField;

    public List<JSONString> jsonBuffer;

    // meta info for a string conversion
    public ValueMetaInterface[] conversionMeta;

    public CiviData() {
        super();

        jsonBuffer = new ArrayList<JSONString>();
    }
}
