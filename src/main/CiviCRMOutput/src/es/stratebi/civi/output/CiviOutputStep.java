package es.stratebi.civi.output;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import es.stratebi.civi.CiviStep;
import es.stratebi.civi.util.RestUtil;

public class CiviOutputStep extends CiviStep implements StepInterface {
    private RestUtil crUtil;

    public CiviOutputStep(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

        civiMeta = (CiviOutputMeta) smi;
        civiData = (CiviOutputData) sdi;

        Object[] inData = getRow(); // get row, blocks when needed!
        if (inData == null) // no more input to be expected...
        {
            setOutputDone();
            // Closing CiviCrm Server connection
            crUtil.closeSocketConnection();
            return false;
        }

        if (first) {
            first = false;
            // determine output field structure
            ((CiviOutputData) civiData).outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
            ((CiviOutputMeta) civiMeta).getFields(((CiviOutputData) civiData).outputRowMeta, getStepname(), null, null, this);
        }
        String params = "";
        String[] fieldNames = getInputRowMeta().getFieldNames();
        for (String field : ((CiviOutputMeta) civiMeta).getKeyList()) {
            params = params + "&" + field;
            int i = 0;
            for(String fName: fieldNames) {
              if (fName.equals(field)) {
                if (inData[i] != null && !inData[i].toString().isEmpty()) {
                  params = params + "=" + inData[i];
                } else {
                  params += "=";
                }
                
              }
              i++;
            }
        }

        // Open CiviCrm server connection and send rows to CiviCrm Rest API 
        String msg = crUtil.putValues(params, "", false);
        if (!msg.equals("")) {
          logError("Error \n" + msg);
          throw new KettleException(BaseMessages.getString(PKG, "CiviCrmStep.Error.OuputData"), new Exception(BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle")));
        }
        
        putRow(((CiviOutputData) civiData).outputRowMeta, inData);

        // Some basic logging
        if (checkFeedback(getLinesRead())) {
            if (log.isBasic())
                logBasic("Linenr " + getLinesRead());
        }

        return true;
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
      boolean passed = super.init(smi, sdi);
      if (passed) {
        crUtil = new RestUtil(
            ((CiviOutputMeta) civiMeta).getCiviCrmRestUrl(), 
            ((CiviOutputMeta) civiMeta).getCiviCrmApiKey(),
            ((CiviOutputMeta) civiMeta).getCiviCrmSiteKey(), 
            ((CiviOutputMeta) civiMeta).getCiviCrmEntity()
            );
        crUtil.setAction("CREATE");
      }
      return passed;
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        super.dispose(smi, sdi);
    }

    @Override
    protected Object[] readOneRow()  throws KettleException {
      return null;
    }

}
