package es.stratebi.civi.output;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class CiviOutputData extends BaseStepData implements StepDataInterface {

	public RowMetaInterface outputRowMeta;
	
	// meta info for a string conversion 
	public ValueMetaInterface[] conversionMeta;
	
    public CiviOutputData()
	{
		super();
	}
}
	
