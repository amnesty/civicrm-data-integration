package es.stratebi.civi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import es.stratebi.civi.util.FieldAttrs;
import es.stratebi.civi.util.RestUtil;

/*
 * 
 * 
 * Este es el dialogo de conexion a la API REST de CiviCRM
 */

public abstract class CiviDialog extends BaseStepDialog implements StepDialogInterface {

  protected static Class<?> PKG = CiviDialog.class; // for i18n purposes

  protected Object input;
  protected Variables variables = new Variables();
  protected String activeEntity = "";

  public TextVar wCiviCrmRestUrl;
  public TextVar wCiviCrmApiKey;
  public TextVar wCiviCrmSiteKey;
  public CCombo wCiviCrmEntity;

  public TableView tOutputFields;

  // the dropdown column which should contain previous fields from stream
  protected String[] comboFieldList = new String[0];
  protected ColumnInfo outputFieldsColumn = null;

  // Constructor
  public CiviDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
    super(parent, (BaseStepMeta) in, transMeta, sname);
    variables.initializeVariablesFrom(null);

    input = in;
  }

  // Construye y muestra el diálogo
  public abstract String open();

  /*
   * Ver comentario del método ok(), no obstante los valores guardados en la
   * clase de intercambio CiviInputMeta son leídos aquí y cargados en el
   * dialogo, pues este método siempre es llamado antes de hacer visible el
   * mismo. Hay que notar que la primera vez que se ejecuta el dialogo al no
   * tener nada guardado se usan los valores establecidos en el método
   * setDefault de CiviInputMeta.
   */
  public void getData() {

    wStepname.selectAll();

    if (((CiviMeta) input).getCiviCrmRestUrl() != null) {
      wCiviCrmRestUrl.setText(((CiviMeta) input).getCiviCrmRestUrl());
    }

    if (((CiviMeta) input).getCiviCrmApiKey() != null) {
      wCiviCrmApiKey.setText(((CiviMeta) input).getCiviCrmApiKey());
    }

    if (((CiviMeta) input).getCiviCrmSiteKey() != null) {
      wCiviCrmSiteKey.setText(((CiviMeta) input).getCiviCrmSiteKey());
    }

    if (((CiviMeta) input).getCiviCrmEntity() != null) {
      wCiviCrmEntity.setText(((CiviMeta) input).getCiviCrmEntity());
      activeEntity = wCiviCrmEntity.getText();
    }

    if (((CiviMeta) input).getCiviCrmEntityList() != null) {
      String[] eArray = (String[]) ((CiviMeta) input).getCiviCrmEntityList().toArray(new String[0]);
      wCiviCrmEntity.setItems(eArray);
    }

    if (((CiviMeta) input).getFields() != null) {

      this.comboFieldList = new String[((CiviMeta) input).getFields().size()];

      int index = 0;
      for (String cField : ((CiviMeta) input).getFields().keySet()) {
        this.comboFieldList[index++] = cField;
      }

      if (((CiviMeta) input).getOutputMap() != null) {
        // Si hay elementos para salida entonces mostrarlos en la tabla
        int i = 0;
        // for (String cFilter : ((CiviMeta) input).getOutputs().keySet()) {
        for (String cField : ((CiviMeta) input).getKeyList()) {
          TableItem item = tOutputFields.table.getItem(i);
          item.setText(1, cField);
          item.setText(2, ((CiviMeta) input).getOutputMap().get(cField));
          i++;
        }
      }
    } else {
      this.comboFieldList = new String[0];
    }

    Arrays.sort(this.comboFieldList);
    this.outputFieldsColumn.setComboValues(this.comboFieldList);

    tOutputFields.removeEmptyRows();
    tOutputFields.setRowNums();
    tOutputFields.optWidth(true);
  }

  /*
   * Aquí se realiza una llamada al API de CIVICRM y se obtiene el listado de
   * campos que admite la entidad, los cuales son desplegados en las tablas que
   * muestran los campos y filtros
   */
  protected void getEntityFields() {
    try {
      if (((CiviMeta) input).getFields().size() > 0) {
        MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
        boolean goOn = MessageDialog.openConfirm(shell, BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Title"),
            BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Msg"));
        if (!goOn) {
          return;
        }
      }
      
      activeEntity = wCiviCrmEntity.getText();
      String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
      String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
      String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());

      RestUtil crUtil = new RestUtil(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

      HashMap<String, FieldAttrs> lFields = crUtil.getFieldLists(true);
      ((CiviMeta) input).setFields(lFields);

      tOutputFields.removeAll();
      this.comboFieldList = new String[lFields.size()];

      int index = 0;
      for (FieldAttrs field : lFields.values()) {
        TableItem outputItem = new TableItem(tOutputFields.table, SWT.NONE);
        outputItem.setText(1, field.getfFieldKey());
        outputItem.setText(2, field.getfFieldKey());

        /*
         * Actualizando elementos desplegables de la tabla
         */
        this.comboFieldList[index++] = field.getfFieldKey();
      }

      Arrays.sort(this.comboFieldList);
      this.outputFieldsColumn.setComboValues(this.comboFieldList);

      tOutputFields.removeEmptyRows();
      tOutputFields.setRowNums();
      tOutputFields.optWidth(true);

    } catch (Exception e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e);
      logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
    }
  }

  protected void testConnection() {
    try {
      String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
      String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
      String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());
      // String entity = wCiviCrmEntity.getText());

      RestUtil crUtil = new RestUtil(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

      crUtil.getEntityList();
      MessageDialog.openInformation(shell, BaseMessages.getString(PKG, "CiviCrmDialog.TestConnection.Title"),
          BaseMessages.getString(PKG, "CiviCrmDialog.TestConnection.Msg"));

    } catch (Exception e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Connection.HttpError"), e.toString().split(":")[0], e); //$NON-NLS-1$ //$NON-NLS-2$
      logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Connection.HttpError", e.toString()));
    }
  }

  protected void getEntities() {
    try {
      if (((CiviMeta) input).getCiviCrmEntityList().size() > 0) {
        MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
        boolean goOn = MessageDialog.openConfirm(shell, BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Title"),
            BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Msg"));
        if (!goOn) {
          return;
        }
      }

      String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
      String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
      String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());
      // String entity = wCiviCrmEntity.getText());

      RestUtil crUtil = new RestUtil(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

      ArrayList<String> entityList = crUtil.getEntityList();
      ((CiviMeta) input).setCiviCrmEntityList(entityList);
      String[] eArray = (String[]) entityList.toArray(new String[0]);
      wCiviCrmEntity.setItems(eArray);
    } catch (Exception e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e);
      logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
    }
  }

  protected void cancel() {
    stepname = null;
    ((CiviMeta) input).setChanged(backupChanged);
    dispose();
  }

  /*
   * Este método define que es lo que va a salir y actualiza la clase
   * CiviInputMeta que contiene los datos necesarios para que funcione
   * adecuadamente el paso. Esto es posible puesto que tenemos una variable
   * privada que declaramos de su mismo tipo y es asignada al dialogo cuando
   * este es creado. Note que este clase se usa como area de intercambio entre
   * Kettle y el diálogo
   */
  protected boolean ok() {

    stepname = wStepname.getText();

    ((CiviMeta) input).setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
    ((CiviMeta) input).setCiviCrmApiKey(wCiviCrmApiKey.getText());
    ((CiviMeta) input).setCiviCrmSiteKey(wCiviCrmSiteKey.getText());
    ((CiviMeta) input).setCiviCrmEntity(wCiviCrmEntity.getText());

    int nrKeys = tOutputFields.nrNonEmpty();
    ArrayList<String> keyList = new ArrayList<String>();

    HashMap<String, String> hOutput = new HashMap<String, String>();

    for (int i = 0; i < nrKeys; i++) {
      TableItem item = tOutputFields.getNonEmpty(i);
      // Verificamos que los elementos de salida siempre tengan un campo
      // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
      // campo
      
      if (item.getText(1) != null && !item.getText(1).equals("")) {
        keyList.add(item.getText(1));
        hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
      }
      
    }

    HashMap<String, FieldAttrs> fields = ((CiviMeta) input).getFields();
    boolean matchFields = true;
    for (String fieldKey : keyList) {
      matchFields = (fields.get(fieldKey) != null);
      if (!matchFields) {
        String msg = "";
           msg = BaseMessages.getString(PKG, "CiviCrmStep.Error.FilterNotMatch")
           .replace("$1", fieldKey)
           .replace("$2", activeEntity);
        Exception e = new Exception();
        new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.UnableFindField"), msg, e);
        break;
      }
    }

    if (matchFields) {
      ((CiviMeta) input).setKeyList(keyList);
      ((CiviMeta) input).setOutputMap(hOutput);
    }
    return matchFields;
  }
}
