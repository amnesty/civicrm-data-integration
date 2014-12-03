package es.stratebi.civi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import es.stratebi.civi.util.CiviField;
import es.stratebi.civi.util.CiviRestService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Button;
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

/*
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
    protected CCombo wCiviCrmEntityAction;
    public CCombo wCiviCrmEntity;

    public TableView tOutputFields;
    // the dropdown column which should contain previous fields from stream
    protected ArrayList<String> comboFieldList = new ArrayList<String>();
    protected ArrayList<String> comboFilterFields = new ArrayList<String>();

    protected ColumnInfo outputFieldsColumn = null;
    protected Button wCiviCrmDebugMode;

    protected TextVar wCiviCrmResultField;
    protected HashMap<String, CiviField> civiCrmFilterFields = new HashMap<String, CiviField>();
    protected HashMap<String, CiviField> civiCrmListingFields = new HashMap<String, CiviField>();
    protected ArrayList<String> civiCrmActionList = new ArrayList<String>();
    protected ArrayList<String> civiCrmEntityList = new ArrayList<String>();
    private String lastEntityAction = "";

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
     * mismo. Hay que notar que la primera vez que se ejecuta el dialogo y al no
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
            civiCrmEntityList.addAll(((CiviMeta) input).getCiviCrmEntityList());
        }

        if (((CiviMeta) input).getCiviCrmAction() != null) {
            wCiviCrmEntityAction.setText(((CiviMeta) input).getCiviCrmAction());
        }

        if (((CiviMeta) input).getCiviCrmActionList() != null) {
            String[] eArray = (String[]) ((CiviMeta) input).getCiviCrmActionList().toArray(new String[0]);
            wCiviCrmEntityAction.setItems(eArray);
            civiCrmActionList.addAll(((CiviMeta) input).getCiviCrmActionList());
        }

        if (((CiviMeta) input).getCiviCrmResultField() != null) {
            wCiviCrmResultField.setText(((CiviMeta) input).getCiviCrmResultField());
        }

        if (((CiviMeta) input).getCiviCrmDebugMode() != null) {
            wCiviCrmDebugMode.setSelection(((CiviMeta) input).getCiviCrmDebugMode());
        }

        civiCrmListingFields.clear();
        if (((CiviMeta) input).getCiviCrmListingFields() != null) {
            civiCrmListingFields.putAll(((CiviMeta) input).getCiviCrmListingFields());
        }

        if (((CiviMeta) input).getCiviCrmListingFields() != null) {

            this.comboFieldList = new ArrayList<String>();
                    //new String[((CiviMeta) input).getCiviCrmListingFields().size()];

            for (String cField : ((CiviMeta) input).getCiviCrmListingFields().keySet()) {
                this.comboFieldList.add(cField);
            }
            Collections.sort(this.comboFieldList);

            if (((CiviMeta) input).getCiviCrmOutputMap() != null) {
                // Si hay elementos para salida entonces mostrarlos en la tabla
                int i = 0;
                for (String cField : ((CiviMeta) input).getCiviCrmKeyList()) {
                    TableItem item = null;
                    // IndexOutOfBounds Exception
                    if (i >= tOutputFields.table.getItemCount()) {
                        item = new TableItem(tOutputFields.table, SWT.NONE);
                    } else {
                        item = tOutputFields.table.getItem(i);
                    }
                    item.setText(0, "");
                    item.setText(1, cField);
                    item.setText(2, ((CiviMeta) input).getCiviCrmOutputMap().get(cField));
                    try {
                        if (this.getClass().getSimpleName().contains("Input")) {
                            CiviField cf = ((CiviMeta) input).getCiviCrmListingFields().get(cField);
                            item.setText(3, (cf != null ? cf.getSource() : ""));
                            item.setText(4, (cf != null ? cf.getTitle() : ""));
                        } else {
                            // Aquí es posible que no coincida el campo de entrada con la salida en caso de
                            // que el campo sea incluído manualmente
                            String fieldName = ((CiviMeta) input).getCiviCrmOutputMap().get(cField);
                            CiviField cf = ((CiviMeta) input).getCiviCrmListingFields().get(fieldName);
                            item.setText(3, (cf != null ? cf.getTitle() : ""));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    i++;
                }
            }
        } else {
            this.comboFieldList.clear();
        }

        this.outputFieldsColumn.setComboValues(this.comboFieldList.toArray(new String[0]));

        tOutputFields.removeEmptyRows();
        tOutputFields.setRowNums();
        tOutputFields.optWidth(true);
    }

    /*
     * Aquí se realiza una llamada al API de CIVICRM y se obtiene el listado de
     * campos que admite la entidad, los cuales son desplegados en las tablas que
     * muestran los campos y filtros
     */
    protected boolean getEntityFieldsAndActions() {
        try {
            if (civiCrmListingFields.size() > 0) {
                MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
                boolean goOn = MessageDialog.openConfirm(shell, BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Title"),
                        BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Msg"));
                if (!goOn) {
                    return false;
                }
            }

            activeEntity = wCiviCrmEntity.getText();
            String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
            String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
            String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());

            CiviRestService crUtil = new CiviRestService(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

            tOutputFields.removeAll();
            this.comboFieldList.clear();
            civiCrmFilterFields.clear();
            civiCrmListingFields.clear();

            civiCrmListingFields =  crUtil.getFieldList(true);
            crUtil.updateReturningFields(civiCrmListingFields, civiCrmFilterFields);

            for (CiviField field : civiCrmListingFields.values()) {
                this.comboFieldList.add(field.getFieldName());
            }
            Collections.sort(this.comboFieldList);

            for (String fieldName : comboFieldList) {
                TableItem outputItem = new TableItem(tOutputFields.table, SWT.END);
                CiviField field = civiCrmListingFields.get(fieldName);

                outputItem.setText(1, field.getFieldName()); // fieldName
                outputItem.setText(2, field.getFieldName()); // renameTo
                outputItem.setText(3, field.getSource());    // returned By
                outputItem.setText(4, field.getTitle());     // description
            }


            this.outputFieldsColumn.setComboValues(this.comboFieldList.toArray(new String[0]));

            tOutputFields.removeEmptyRows();
            tOutputFields.setRowNums();
            tOutputFields.optWidth(true);

            crUtil.setAction("getactions");
            civiCrmActionList = crUtil.getEntityActions(true);

            String[] eArray = (String[]) civiCrmActionList.toArray(new String[0]);
            wCiviCrmEntityAction.setItems(eArray);
        } catch (Exception e) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e);
            logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
        }
        return true;
    }

    /*
     * Aquí se realiza una llamada al API de CIVICRM y se obtiene el listado de
     * campos que admite la entidad, los cuales son desplegados en las tablas que
     * muestran los campos y filtros
     */
    protected boolean updateEntityFieldsForAction() {
        if (!lastEntityAction.equals(wCiviCrmEntityAction.getText())) {
            lastEntityAction = wCiviCrmEntityAction.getText();
            tOutputFields.removeAll();

            if (wCiviCrmEntityAction.getText().equals("getcount")) {
                TableItem outputItem = new TableItem(tOutputFields.table, SWT.END);
                outputItem.setText(1, "result");
            } else if (wCiviCrmEntityAction.getText().equals("getoptions")) {
                TableItem outputItem = new TableItem(tOutputFields.table, SWT.END);
                outputItem.setText(1, "key");
                outputItem = new TableItem(tOutputFields.table, SWT.END);
                outputItem.setText(1, "value");
            } else {
                for (CiviField field : civiCrmListingFields.values()) {
                    TableItem outputItem = new TableItem(tOutputFields.table, SWT.END);
                    outputItem.setText(1, field.getFieldName());
                    outputItem.setText(2, field.getFieldName());
                    outputItem.setText(3, field.getSource());
                    outputItem.setText(4, field.getTitle());

                    this.comboFieldList.add(field.getFieldName());
                }

            }
            tOutputFields.removeEmptyRows();
            tOutputFields.setRowNums();
            tOutputFields.optWidth(true);
        }
        return true;
    }

    protected void testConnection() {
        try {
            String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
            String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
            String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());
            // String entity = wCiviCrmEntity.getText());

            CiviRestService crUtil = new CiviRestService(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

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
            if (civiCrmEntityList != null && civiCrmEntityList.size() > 0) {
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

            CiviRestService crUtil = new CiviRestService(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

            civiCrmEntityList = crUtil.getEntityList();
            String[] eArray = (String[]) civiCrmEntityList.toArray(new String[0]);
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
        ((CiviMeta) input).setCiviCrmDebugMode(wCiviCrmDebugMode.getSelection());

        int nrKeys = tOutputFields.nrNonEmpty();
        ArrayList<String> keyList = new ArrayList<String>();

        HashMap<String, String> hOutput = new HashMap<String, String>();

        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tOutputFields.getNonEmpty(i);
            // Verificamos que los elementos de salida siempre tengan un campo
            // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
            // campo

            if (item.getText(1) != null && !item.getText(1).equals("")) {
                if (keyList.contains(item.getText(1))) {
                    new ErrorDialog(shell,
                            BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle"),
                            BaseMessages.getString(PKG, "CiviCrmStep.Error.DuplicateKeyField"), new Exception());
                    return false;
                } else if (hOutput.values().contains(((item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1)))) {
                    new ErrorDialog(shell,
                            BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle"),
                            BaseMessages.getString(PKG, "CiviCrmStep.Error.DuplicateOutputField"), new Exception());
                    return false;
                } else {
                    keyList.add(item.getText(1));
                    hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
                }
            }
        }

        HashMap<String, CiviField> fields = ((CiviMeta) input).getCiviCrmListingFields();
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
            ((CiviMeta) input).setCiviCrmKeyList(keyList);
            ((CiviMeta) input).setCiviCrmOutputMap(hOutput);
        }
        return matchFields;
    }
}
