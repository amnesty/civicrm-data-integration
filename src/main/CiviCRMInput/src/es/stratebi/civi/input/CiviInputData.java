package es.stratebi.civi.input;

import org.pentaho.di.trans.step.StepDataInterface;

import es.stratebi.civi.CiviData;

/*
 * 
 * Esta clase es la que mantiene los datos que se van a enviar hacia el paso que sigue. El truco que
 * hemos usado es crear una lista con todos aquellos valores que devueltos por el API de CIVICRM
 * para ir eliminandolos en cada llamado del m�todo readOneRow llamado por processRow.
 */
public class CiviInputData extends CiviData implements StepDataInterface {

    public CiviInputData() {
        super();
    }
}
