package es.stratebi.civi.input;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import es.stratebi.civi.util.CiviField;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import es.stratebi.civi.CiviDialog;

/*
 * 
 * Este es el dialogo de conexion a la API REST de CiviCRM
 */

public class CiviInputDialog extends CiviDialog implements StepDialogInterface {

    private TextVar wCiviCrmPageSize;

    // lookup fields settings widgets
    private TableView tFilterFields;
    private ColumnInfo filterFieldsColumn = null;

    // all fields from the previous steps, used for drop down selection
    private RowMetaInterface prevMetaFields = null;
    private String[] prevFields = new String[0];

    // the drop down column which should contain previous fields from stream
    private ColumnInfo prevFieldColumn = null;

    private Group gEntity;

    private Button wGetEntities;
    @SuppressWarnings("FieldCanBeLocal")
    private Button wEntityListBtn;

    private ColumnInfo operatorFieldColumn;
    private int middle;
    private int margin;
    private ModifyListener lsMod;
    private Button wCiviCrmPassRowOnFail;
    private CCombo wCiviCrmOnMultipleRows;
    private CCombo wCiviCrmOptionFields;
    private ArrayList<String> comboAllFieldList;

    // Constructor
    public CiviInputDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
        super(parent, in, transMeta, sname);
    }

    // Construye y muestra el diálogo
    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        props.setLook(shell);
        setShellImage(shell, (StepMetaInterface) input);

        lsMod = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                ((CiviInputMeta) input).setChanged();
            }
        };
        backupChanged = ((CiviInputMeta) input).hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "CiviCrmDialog.Shell.Input.Title"));

        middle = props.getMiddlePct();
        margin = Const.MARGIN;

        /*************************************************
         * // STEP NAME ENTRY
         *************************************************/

        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText(BaseMessages.getString(PKG, "System.Label.StepName"));
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);

        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(lsMod);
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment(middle, 0);
        fdStepname.top = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        addOKCancelButtons();
        addConnectionTab();
        addEntityGroup();
        addOutputTab();

//        wCiviCrmRestUrl.setText("${REST_URL}");
//        wCiviCrmSiteKey.setText("${SITE_KEY}");
//        wCiviCrmApiKey.setText("${API_KEY}");

        /*************************************************
         * // DEFAULT ACTION LISTENERS
         *************************************************/


        lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };

        wStepname.addSelectionListener(lsDef);
        wCiviCrmRestUrl.addSelectionListener(lsDef);
        wCiviCrmRestUrl.addSelectionListener(lsDef);
        wCiviCrmSiteKey.addSelectionListener(lsDef);
        wCiviCrmEntity.addSelectionListener(lsDef);

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener(new ShellAdapter() {
            public void shellClosed(ShellEvent e) {
                cancel();
            }
        });

        // Set the shell size, based upon previous time...
        setSize();

        /*************************************************
         * // POPULATE AND OPEN DIALOG
         *************************************************/

        getData();
        // setComboValues();

        ((CiviInputMeta) input).setChanged(backupChanged);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        return stepname;
    }

    private void addOKCancelButtons() {
        /*************************************************
         * // OK AND CANCEL BUTTONS
         *************************************************/

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wPreview = new Button(shell, SWT.PUSH);
        wPreview.setText(BaseMessages.getString(PKG, "System.Button.Preview"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        BaseStepDialog.positionBottomButtons(shell, new Button[]{wOK, wPreview, wCancel}, margin, null);

        // Add listeners
        lsCancel = new Listener() {
            public void handleEvent(Event e) {
                cancel();
            }
        };
        lsOK = new Listener() {
            public void handleEvent(Event e) {
                ok();
            }
        };
        lsPreview = new Listener() {
            public void handleEvent(Event e) {
                preview();
            }
        };

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);
        wPreview.addListener(SWT.Selection, lsPreview);
    }

    private void addConnectionTab() {
        CTabFolder wConnectionFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(wConnectionFolder, Props.WIDGET_STYLE_TAB);

        /*************************************************
         * // CIVICRM CONNECTION TAB
         *************************************************/

        CTabItem wConnectionTab = new CTabItem(wConnectionFolder, SWT.NONE);
        wConnectionTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ConnectionTab.Label"));

        Composite gConnectionFields = new Composite(wConnectionFolder, SWT.NONE);
        props.setLook(gConnectionFields);

        FormLayout connectionFieldsCompLayout = new FormLayout();
        connectionFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        connectionFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gConnectionFields.setLayout(connectionFieldsCompLayout);

        /************************************************************************************/
        // CiviCrm RestUrl
        Label wlCiviCrmRestUrl = new Label(gConnectionFields, SWT.RIGHT);
        wlCiviCrmRestUrl.setText(BaseMessages.getString(PKG, "CiviCrmDialog.RestURL.Label"));
        props.setLook(wlCiviCrmRestUrl);

        FormData fdlCiviCrmRestUrl = new FormData();
        fdlCiviCrmRestUrl.top = new FormAttachment(0, margin);
        fdlCiviCrmRestUrl.left = new FormAttachment(0, 0);
        fdlCiviCrmRestUrl.right = new FormAttachment(middle, -margin);
        wlCiviCrmRestUrl.setLayoutData(fdlCiviCrmRestUrl);

        wCiviCrmRestUrl = new TextVar(transMeta, gConnectionFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wCiviCrmRestUrl.addModifyListener(lsMod);
        wCiviCrmRestUrl.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.RestURL.Tooltip"));
        props.setLook(wCiviCrmRestUrl);

        FormData fdCiviCrmRestUrl = new FormData();
        fdCiviCrmRestUrl.top = new FormAttachment(0, margin);
        fdCiviCrmRestUrl.left = new FormAttachment(middle, 0);
        fdCiviCrmRestUrl.right = new FormAttachment(100, 0);
        wCiviCrmRestUrl.setLayoutData(fdCiviCrmRestUrl);

        // CiviCrm SiteKey
        Label wlCiviCrmSiteKey = new Label(gConnectionFields, SWT.RIGHT);
        wlCiviCrmSiteKey.setText(BaseMessages.getString(PKG, "CiviCrmDialog.SiteKey.Label"));
        props.setLook(wlCiviCrmSiteKey);

        FormData fdlCiviCrmSiteKey = new FormData();
        fdlCiviCrmSiteKey.top = new FormAttachment(wCiviCrmRestUrl, margin);
        fdlCiviCrmSiteKey.left = new FormAttachment(0, 0);
        fdlCiviCrmSiteKey.right = new FormAttachment(middle, -margin);
        wlCiviCrmSiteKey.setLayoutData(fdlCiviCrmSiteKey);

        wCiviCrmSiteKey = new TextVar(transMeta, gConnectionFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wCiviCrmSiteKey.addModifyListener(lsMod);
        wCiviCrmSiteKey.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.SiteKey.Tooltip"));
        props.setLook(wCiviCrmSiteKey);

        FormData fdCiviCrmSiteKey = new FormData();
        fdCiviCrmSiteKey.top = new FormAttachment(wCiviCrmRestUrl, margin);
        fdCiviCrmSiteKey.left = new FormAttachment(middle, 0);
        fdCiviCrmSiteKey.right = new FormAttachment(100, 0);
        wCiviCrmSiteKey.setLayoutData(fdCiviCrmSiteKey);

        // CiviCrm ApiKey
        Label wlCiviCrmApiKey = new Label(gConnectionFields, SWT.RIGHT);
        wlCiviCrmApiKey.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ApiKey.Label"));
        props.setLook(wlCiviCrmApiKey);

        FormData fdlCiviCrmApiKey = new FormData();
        fdlCiviCrmApiKey.top = new FormAttachment(wCiviCrmSiteKey, margin);
        fdlCiviCrmApiKey.left = new FormAttachment(0, 0);
        fdlCiviCrmApiKey.right = new FormAttachment(middle, -margin);
        wlCiviCrmApiKey.setLayoutData(fdlCiviCrmApiKey);

        wCiviCrmApiKey = new TextVar(transMeta, gConnectionFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wCiviCrmApiKey.addModifyListener(lsMod);
        wCiviCrmApiKey.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.ApiKey.Tooltip"));
        props.setLook(wCiviCrmApiKey);

        FormData fdCiviCrmApiKey = new FormData();
        fdCiviCrmApiKey.top = new FormAttachment(wCiviCrmSiteKey, margin);
        fdCiviCrmApiKey.left = new FormAttachment(middle, 0);
        fdCiviCrmApiKey.right = new FormAttachment(100, 0);
        wCiviCrmApiKey.setLayoutData(fdCiviCrmApiKey);

        /****************************************************************************/
        FormData fdConnectionTabFolder = new FormData();
        fdConnectionTabFolder.left = new FormAttachment(0, 0);
        fdConnectionTabFolder.top = new FormAttachment(0, 0);
        fdConnectionTabFolder.right = new FormAttachment(100, 0);
        fdConnectionTabFolder.bottom = new FormAttachment(100, 0);
        gConnectionFields.setLayoutData(fdConnectionTabFolder);

        wConnectionFolder.layout();
        wConnectionTab.setControl(gConnectionFields);
        /*************************************************
         * // CIVICRM PERFOMANCE TAB
         *************************************************/

        CTabItem wPerfomanceTab = new CTabItem(wConnectionFolder, SWT.NONE);
        wPerfomanceTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.PerfomanceTab.Label"));

        Composite gPerfomanceFields = new Composite(wConnectionFolder, SWT.NONE);
        props.setLook(gPerfomanceFields);

        FormLayout perfomanceFieldsCompLayout = new FormLayout();
        connectionFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        connectionFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gPerfomanceFields.setLayout(perfomanceFieldsCompLayout);

        /****************************************************************************/
        // CiviCrm pageSize label
        Label wlCiviCrmPageSize = new Label(gPerfomanceFields, SWT.RIGHT);
        wlCiviCrmPageSize.setText(BaseMessages.getString(PKG, "CiviCrmDialog.PageSize.Label"));
        props.setLook(wlCiviCrmPageSize);

        FormData fdlCiviPageSize = new FormData();
        fdlCiviPageSize.top = new FormAttachment(wCiviCrmApiKey, margin);
        fdlCiviPageSize.left = new FormAttachment(0, 0);
        fdlCiviPageSize.right = new FormAttachment(middle, -margin);
        wlCiviCrmPageSize.setLayoutData(fdlCiviPageSize);

        // CiviCrm pageSize text
        wCiviCrmPageSize = new TextVar(transMeta, gPerfomanceFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wCiviCrmPageSize.addModifyListener(lsMod);
        wCiviCrmPageSize.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.PageSize.Tooltip"));
        props.setLook(wCiviCrmPageSize);

        FormData fdCiviPageSize = new FormData();
        fdCiviPageSize.top = new FormAttachment(wCiviCrmApiKey, margin);
        fdCiviPageSize.left = new FormAttachment(middle, 0);
        fdCiviPageSize.right = new FormAttachment(100, 0);
        wCiviCrmPageSize.setLayoutData(fdCiviPageSize);

        // CiviCrm JSON ResultField
        Label wlCiviCrmResultField = new Label(gPerfomanceFields, SWT.RIGHT);
        wlCiviCrmResultField.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ResultFieldName.Label"));
        props.setLook(wlCiviCrmResultField);

        FormData fdlCiviCrmApiResultField = new FormData();
        fdlCiviCrmApiResultField.top = new FormAttachment(wCiviCrmPageSize, margin);
        fdlCiviCrmApiResultField.left = new FormAttachment(0, 0);
        fdlCiviCrmApiResultField.right = new FormAttachment(middle, -margin);
        wlCiviCrmResultField.setLayoutData(fdlCiviCrmApiResultField);

        wCiviCrmResultField = new TextVar(transMeta, gPerfomanceFields, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wCiviCrmResultField.addModifyListener(lsMod);
        wCiviCrmResultField.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.ResultFieldName.Tooltip"));
        props.setLook(wCiviCrmResultField);

        FormData fdCiviCrmApiResultField = new FormData();
        fdCiviCrmApiResultField.top = new FormAttachment(wCiviCrmPageSize, margin);
        fdCiviCrmApiResultField.left = new FormAttachment(middle, 0);
        fdCiviCrmApiResultField.right = new FormAttachment(100, 0);
        wCiviCrmResultField.setLayoutData(fdCiviCrmApiResultField);

        // OnMultipleRows selection
        Label wlOnMultipleRowsLabel = new Label(gPerfomanceFields, SWT.RIGHT);
        wlOnMultipleRowsLabel.setText(BaseMessages.getString(PKG, "CiviCrmDialog.OnMultipleRows.Label"));
        props.setLook(wlOnMultipleRowsLabel);

        FormData fdlCiviCrmOnMultipleRows = new FormData();
        fdlCiviCrmOnMultipleRows.top = new FormAttachment(wCiviCrmResultField, margin);
        fdlCiviCrmOnMultipleRows.left = new FormAttachment(0, 0);
        fdlCiviCrmOnMultipleRows.right = new FormAttachment(middle, -margin);
        wlOnMultipleRowsLabel.setLayoutData(fdlCiviCrmOnMultipleRows);

        wCiviCrmOnMultipleRows = new CCombo(gPerfomanceFields, SWT.BORDER);
        wCiviCrmOnMultipleRows.addModifyListener(lsMod);
        wCiviCrmOnMultipleRows.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.OnMultipleRows.Tooltip"));
        props.setLook(wCiviCrmOnMultipleRows);

        FormData fdOnMultipleRowsCombo = new FormData();
        fdOnMultipleRowsCombo.top = new FormAttachment(wCiviCrmResultField, margin);
        fdOnMultipleRowsCombo.left = new FormAttachment(middle, 0);
        fdOnMultipleRowsCombo.right = new FormAttachment(55, -margin);
        wCiviCrmOnMultipleRows.setLayoutData(fdOnMultipleRowsCombo);
        String[] actions = BaseMessages.getString(PKG, "CiviCrmDialog.OnMultipleRows.Items").split(",");
        wCiviCrmOnMultipleRows.setItems(actions);
        wCiviCrmOnMultipleRows.setText(actions[0]);


        // CiviCrm pass row label
        Label wlCiviCrmPassRowOnFail = new Label(gPerfomanceFields, SWT.RIGHT);
        wlCiviCrmPassRowOnFail.setText(BaseMessages.getString(PKG, "CiviCrmDialog.PassRowOnFail.Label"));
        props.setLook(wlCiviCrmPassRowOnFail);

        FormData fdlPassRowOnFail = new FormData();
        fdlPassRowOnFail.top = new FormAttachment(wCiviCrmOnMultipleRows, margin);
        fdlPassRowOnFail.right = new FormAttachment(middle, -margin);
        fdlPassRowOnFail.left = new FormAttachment(0, 0);
        wlCiviCrmPassRowOnFail.setLayoutData(fdlPassRowOnFail);

        // CiviCrm pass row checkbox
        wCiviCrmPassRowOnFail = new Button(gPerfomanceFields, SWT.CHECK);
        wCiviCrmPassRowOnFail.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.PassRowOnFail.Tooltip"));
        props.setLook(wCiviCrmPassRowOnFail);

        FormData fdCiviCrmdPassRowOnFail = new FormData();
        fdCiviCrmdPassRowOnFail.top = new FormAttachment(wCiviCrmOnMultipleRows, margin);
        fdCiviCrmdPassRowOnFail.right = new FormAttachment(wlCiviCrmPassRowOnFail, 25, SWT.RIGHT);
        fdCiviCrmdPassRowOnFail.left = new FormAttachment(wlCiviCrmPassRowOnFail, 5);

        wCiviCrmPassRowOnFail.setLayoutData(fdCiviCrmdPassRowOnFail);

        // CiviCrm debug mode label
        Label wlCiviCrmDebugMode = new Label(gPerfomanceFields, SWT.RIGHT);
        wlCiviCrmDebugMode.setText(BaseMessages.getString(PKG, "CiviCrmDialog.DebugMode.Label"));
        props.setLook(wlCiviCrmDebugMode);

        FormData fdlDebugMode = new FormData();
        fdlDebugMode.top = new FormAttachment(wCiviCrmOnMultipleRows, margin);
        fdlDebugMode.right = new FormAttachment(65, -margin);
        fdlDebugMode.left = new FormAttachment(0, 0);
        wlCiviCrmDebugMode.setLayoutData(fdlDebugMode);

        // CiviCrm debug mode checkbox
        wCiviCrmDebugMode = new Button(gPerfomanceFields, SWT.CHECK);
        wCiviCrmDebugMode.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.DebugMode.Tooltip"));
        props.setLook(wCiviCrmDebugMode);

        FormData fdCiviCrmdDebugMode = new FormData();
        fdCiviCrmdDebugMode.top = new FormAttachment(wCiviCrmOnMultipleRows, margin);
        fdCiviCrmdDebugMode.right = new FormAttachment(wlCiviCrmDebugMode, 25, SWT.RIGHT);
        fdCiviCrmdDebugMode.left = new FormAttachment(wlCiviCrmDebugMode, 5);

        wCiviCrmDebugMode.setLayoutData(fdCiviCrmdDebugMode);
        /****************************************************************************/

        FormData fdPerfomanceTabFolder = new FormData();
        fdConnectionTabFolder.left = new FormAttachment(0, 0);
        fdConnectionTabFolder.top = new FormAttachment(0, 0);
        fdConnectionTabFolder.right = new FormAttachment(100, 0);
        fdConnectionTabFolder.bottom = new FormAttachment(100, 0);
        gPerfomanceFields.setLayoutData(fdPerfomanceTabFolder);

        wConnectionFolder.layout();
        wPerfomanceTab.setControl(gPerfomanceFields);

        FormData fdConnectionFolder = new FormData();
        fdConnectionFolder.left  = new FormAttachment(0, 0);
        fdConnectionFolder.top   = new FormAttachment(wStepname, margin);
        fdConnectionFolder.right = new FormAttachment(100, 0);
        wConnectionFolder.setLayoutData(fdConnectionFolder);

        wConnectionFolder.setSelection(0);

        /*********************************************************************
         * Get entity fields button
         *********************************************************************/

        wGetEntities = new Button(shell, SWT.PUSH);
        wGetEntities.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityList.Button"));

        FormData fdGetEntities = new FormData();
        fdGetEntities.top = new FormAttachment(wConnectionFolder, margin);
        fdGetEntities.right = new FormAttachment(100, 0);
        wGetEntities.setLayoutData(fdGetEntities);

        Listener lsGetEntities = new Listener() {
            public void handleEvent(Event e) {
                getEntities();
            }
        };

        wGetEntities.addListener(SWT.Selection, lsGetEntities);
    }

    private void addEntityGroup() {
        /*************************************************
         * // CIVICRM ENTITY GROUP
         *************************************************/

        gEntity = new Group(shell, SWT.NONE);
        gEntity.setText(BaseMessages.getString(PKG, "CiviCrmDialog.EntityGroup.Label"));
        props.setLook(gEntity);

        FormLayout frmEntityLayout = new FormLayout();
        frmEntityLayout.marginWidth = 3;
        frmEntityLayout.marginHeight = 3;
        gEntity.setLayout(frmEntityLayout);

        FormData fdEntity = new FormData();
        fdEntity.left = new FormAttachment(0, 0);
        fdEntity.right = new FormAttachment(100, 0);
        fdEntity.top = new FormAttachment(wGetEntities, margin);
        gEntity.setLayoutData(fdEntity);

        // -----------------------------------------------

        // CiviCrm Entity name
        Label wlCiviCrmEntity = new Label(gEntity, SWT.RIGHT);
        wlCiviCrmEntity.setText(BaseMessages.getString(PKG, "CiviCrmDialog.Entity.Label"));
        props.setLook(wlCiviCrmEntity);

        FormData fdlCiviCrmEntity = new FormData();
        fdlCiviCrmEntity.top = new FormAttachment(0, margin);
        fdlCiviCrmEntity.left = new FormAttachment(0, 0);
        fdlCiviCrmEntity.right = new FormAttachment(middle, -margin);
        wlCiviCrmEntity.setLayoutData(fdlCiviCrmEntity);

        wCiviCrmEntity = new CCombo(gEntity, SWT.BORDER);
        wCiviCrmEntity.addModifyListener(lsMod);
        wCiviCrmEntity.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.Entity.Tooltip"));
        props.setLook(wCiviCrmEntity);

        // Get entity fields button
        wEntityListBtn = new Button(gEntity, SWT.PUSH);
        wEntityListBtn.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityFields.Button"));
        FormData fdGetFields = new FormData();
        fdGetFields.top = new FormAttachment(gEntity, margin);
        fdGetFields.right = new FormAttachment(100, 0);
        fdGetFields.bottom = new FormAttachment(100, 0);
        wEntityListBtn.setLayoutData(fdGetFields);

        FormData fdCiviCrmEntity = new FormData();
        fdCiviCrmEntity.top = new FormAttachment(gEntity, margin);
        fdCiviCrmEntity.left = new FormAttachment(middle, 0);
        fdCiviCrmEntity.right = new FormAttachment(wEntityListBtn, -margin);
        wCiviCrmEntity.setLayoutData(fdCiviCrmEntity);

        wCiviCrmEntity.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                ((CiviInputMeta) input).setChanged();
            }
        });

        // Add listeners
        Listener lsGetFields = new Listener() {
            public void handleEvent(Event e) {
                getEntityFieldsAndActions();
            }
        };
        wEntityListBtn.addListener(SWT.Selection, lsGetFields);

        // CiviCrm action name
        Label wlCiviCrmEntityAction = new Label(gEntity, SWT.RIGHT);
        wlCiviCrmEntityAction.setText(BaseMessages.getString(PKG, "CiviCrmDialog.EntityAction.Label"));
        props.setLook(wlCiviCrmEntityAction);

        FormData fdlCiviCrmEntityAction = new FormData();
        fdlCiviCrmEntityAction.top = new FormAttachment(wCiviCrmEntity, margin);
        fdlCiviCrmEntityAction.left = new FormAttachment(0, 0);
        fdlCiviCrmEntityAction.right = new FormAttachment(middle, -margin);
        wlCiviCrmEntityAction.setLayoutData(fdlCiviCrmEntityAction);

        wCiviCrmEntityAction = new CCombo(gEntity, SWT.BORDER);
        wCiviCrmEntityAction.addModifyListener(lsMod);
        wCiviCrmEntityAction.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.EntityAction.Tooltip"));
        props.setLook(wCiviCrmEntityAction);

        FormData fdCiviCrmEntityAction = new FormData();
        fdCiviCrmEntityAction.top = new FormAttachment(wCiviCrmEntity, margin);
        fdCiviCrmEntityAction.left = new FormAttachment(middle, 0);
        fdCiviCrmEntityAction.right = new FormAttachment(wEntityListBtn, -margin);
        wCiviCrmEntityAction.setLayoutData(fdCiviCrmEntityAction);

        // CiviCrm option field
        Label wlCiviCrmOptionField = new Label(gEntity, SWT.RIGHT);
        wlCiviCrmOptionField.setText(BaseMessages.getString(PKG, "CiviCrmDialog.OptionField.Label"));
        props.setLook(wlCiviCrmOptionField);

        FormData fdlCiviCrmOptionField = new FormData();
        fdlCiviCrmOptionField.top = new FormAttachment(wCiviCrmEntityAction, margin);
        fdlCiviCrmOptionField.left = new FormAttachment(0, 0);
        fdlCiviCrmOptionField.right = new FormAttachment(middle, -margin);
        wlCiviCrmOptionField.setLayoutData(fdlCiviCrmOptionField);

        wCiviCrmOptionFields = new CCombo(gEntity, SWT.BORDER);
        wCiviCrmOptionFields.addModifyListener(lsMod);
        wCiviCrmOptionFields.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.OptionField.Tooltip"));
        props.setLook(wCiviCrmOptionFields);

        FormData fdCiviCrmOptionFields = new FormData();
        fdCiviCrmOptionFields.top = new FormAttachment(wCiviCrmEntityAction, margin);
        fdCiviCrmOptionFields.left = new FormAttachment(middle, 0);
        fdCiviCrmOptionFields.right = new FormAttachment(wEntityListBtn, -margin);
        wCiviCrmOptionFields.setLayoutData(fdCiviCrmOptionFields);
        wCiviCrmOptionFields.setEnabled(false);

        // Add listeners
        wCiviCrmEntityAction.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEntityFieldsForAction();
            }
        });
    }

    protected boolean updateEntityFieldsForAction() {
        boolean result = super.updateEntityFieldsForAction();

        if (wCiviCrmEntityAction.getText().equals("getoptions")) {
            wCiviCrmOptionFields.setEnabled(true);
        } else {
            wCiviCrmOptionFields.setEnabled(false);
        }
        return result;
    }

    private void addOutputTab() {
        CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

        /*************************************************
         * // CIVICRM OUTPUT TAB
         *************************************************/

        CTabItem wOutputFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
        wOutputFieldsTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.OutputFieldsGroup.Title"));

        Composite gOutputFields = new Composite(wTabFolder, SWT.NONE);
        props.setLook(gOutputFields);

        FormLayout outputFieldsCompLayout = new FormLayout();
        outputFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        outputFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gOutputFields.setLayout(outputFieldsCompLayout);

        /*************************************************
         * // KEY / OUTPUT TABLE
         *************************************************/

        int outputKeyWidgetCols = 4;
        int outputKeyWidgetRows = (((CiviInputMeta) input).getCiviCrmListingFields() != null ? ((CiviInputMeta) input).getCiviCrmListingFields().values().size() : 3);

        ColumnInfo[] ciOutputKeys = new ColumnInfo[outputKeyWidgetCols];
        ciOutputKeys[0] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.NameField"), ColumnInfo.COLUMN_TYPE_CCOMBO,new String[] {}, false);
        ciOutputKeys[1] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.RenameField"), ColumnInfo.COLUMN_TYPE_TEXT,false);
        ciOutputKeys[2] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.Source"), ColumnInfo.COLUMN_TYPE_NONE,false);
        ciOutputKeys[3] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.TitleField"), ColumnInfo.COLUMN_TYPE_NONE,false);

        outputFieldsColumn = ciOutputKeys[0];

        tOutputFields = new TableView(transMeta, gOutputFields, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
                ciOutputKeys, outputKeyWidgetRows, lsMod, props);

        FormData fdOutputFields = new FormData();
        fdOutputFields.left = new FormAttachment(0, 0);
        fdOutputFields.top = new FormAttachment(0, margin);
        fdOutputFields.right = new FormAttachment(100, -margin);
        fdOutputFields.bottom = new FormAttachment(100, -margin);
        tOutputFields.setLayoutData(fdOutputFields);

        FormData fdOutputFieldsComp = new FormData();
        fdOutputFieldsComp.left = new FormAttachment(0, 0);
        fdOutputFieldsComp.top = new FormAttachment(0, 0);
        fdOutputFieldsComp.right = new FormAttachment(100, 0);
        fdOutputFieldsComp.bottom = new FormAttachment(100, 0);
        gOutputFields.setLayoutData(fdOutputFieldsComp);

        gOutputFields.layout();
        wOutputFieldsTab.setControl(gOutputFields);


        /*************************************************
         * CIVICRM FILTER TAB
         *************************************************/

        CTabItem wFilterFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
        wFilterFieldsTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.FilterGroup.Title"));

        Composite gEntityFilter = new Composite(wTabFolder, SWT.NONE);
        props.setLook(gEntityFilter);

        FormLayout entityFormLayout = new FormLayout();
        entityFormLayout.marginWidth = Const.FORM_MARGIN;
        entityFormLayout.marginHeight = Const.FORM_MARGIN;
        gEntityFilter.setLayout(entityFormLayout);

        /*************************************************
         * // KEY / FILTER TABLE
         *************************************************/

        int keyWidgetCols = 3;
        int keyWidgetRows = 3;

        ColumnInfo[] ciKeys = new ColumnInfo[keyWidgetCols];
        ciKeys[0] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.NameField"), ColumnInfo.COLUMN_TYPE_CCOMBO,
                new String[] {}, false);
        ciKeys[1] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FilterOperator"), ColumnInfo.COLUMN_TYPE_CCOMBO);
        ciKeys[2] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.InputValueName"), ColumnInfo.COLUMN_TYPE_CCOMBO);

        filterFieldsColumn = ciKeys[0];
        operatorFieldColumn = ciKeys[1];
        prevFieldColumn = ciKeys[2];
        prevFieldColumn.setToolTip(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FilterFieldTooltip"));

        // Chequear si el dato que se modifica es uno de los campos de entrada en ese caso hay que
        // desabilitar el botón preview
        ModifyListener lsFilterMod = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                ((CiviInputMeta) input).setChanged();
                int nrKeys = tFilterFields.nrNonEmpty();
                boolean enabled = true;
                for (int i = 0; i < nrKeys; i++) {
                    TableItem item = tFilterFields.getNonEmpty(i);

                    String filterKey = item.getText(3);
                    for (String prevField : prevFields) {
                        if (filterKey.equals(prevField)) {
                            enabled = false;
                            break;
                        }
                    }
                    if (!enabled) break;
                }
                wPreview.setEnabled(enabled);
            }
        };

        tFilterFields = new TableView(transMeta, gEntityFilter, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
                ciKeys, keyWidgetRows, lsFilterMod, props);


        FormData fdFields = new FormData();
        fdFields.left = new FormAttachment(0, 0);
        fdFields.top = new FormAttachment(0, margin);
        fdFields.right = new FormAttachment(100, -margin);
        fdFields.bottom = new FormAttachment(100, -margin);
        tFilterFields.setLayoutData(fdFields);

        FormData fdFilterFieldsComp = new FormData();
        fdFilterFieldsComp.left = new FormAttachment(0, 0);
        fdFilterFieldsComp.top = new FormAttachment(gEntityFilter, 0);
        fdFilterFieldsComp.right = new FormAttachment(100, 0);
        fdFilterFieldsComp.bottom = new FormAttachment(100, 0);
        gEntityFilter.setLayoutData(fdFilterFieldsComp);

        gEntityFilter.layout();
        wFilterFieldsTab.setControl(gEntityFilter);

        FormData fdTabFolder = new FormData();
        fdTabFolder.left = new FormAttachment(0, 0);
        fdTabFolder.top = new FormAttachment(gEntity, margin);
        fdTabFolder.right = new FormAttachment(100, 0);
        fdTabFolder.bottom = new FormAttachment(wOK, -margin);
        wTabFolder.setLayoutData(fdTabFolder);

        wTabFolder.setSelection(0);
    }

    protected void updatePreviousFields() {
        try {
            prevMetaFields = transMeta.getPrevStepFields(stepname);

            if (prevMetaFields != null && !prevMetaFields.isEmpty()) {
                prevFields = prevMetaFields.getFieldNames();
            } else {
                prevFields = new String[0];
            }

            int nrKeys = tFilterFields.nrNonEmpty();
            boolean enabled = true;
            for (int i = 0; i < nrKeys; i++) {
                TableItem item = tFilterFields.getNonEmpty(i);

                String filterKey = item.getText(3);
                for (String prevField : prevFields) {
                    if (filterKey.equals(prevField)) {
                        enabled = false;
                        break;
                    }
                }
                if (!enabled) break;
            }
            wPreview.setEnabled(enabled);

        } catch (KettleStepException e) {
            e.printStackTrace();
        }
        prevFieldColumn.setComboValues(prevFields);
    }

    protected boolean getEntityFieldsAndActions() {
        try {
            if (!super.getEntityFieldsAndActions()) {
                return false;
            }

            wCiviCrmEntityAction.setText("get");
            wCiviCrmOptionFields.setEnabled(false);

            tFilterFields.clearAll();
            tFilterFields.removeEmptyRows();
            comboFilterFields.clear();
            comboAllFieldList.clear();

            for (CiviField field : civiCrmFilterFields.values()) {
                this.comboFilterFields.add(field.getFieldName());
            }

            Collections.sort(this.comboFilterFields);
            comboAllFieldList.addAll(comboFilterFields);
            wCiviCrmOptionFields.setText("");
            wCiviCrmOptionFields.setItems(comboAllFieldList.toArray(new String[comboAllFieldList.size()]));

            for (String field : this.comboFilterFields) {
                TableItem outputItem = new TableItem(tFilterFields.table, SWT.END);
                outputItem.setText(1, field);
            }

            filterFieldsColumn.setComboValues(this.comboFilterFields.toArray(new String[this.comboFilterFields.size()]));

            prevMetaFields = transMeta.getPrevStepFields(stepname);

            boolean enablePreview = true;
            if (prevMetaFields != null && !prevMetaFields.isEmpty()) {
                prevFields = prevMetaFields.getFieldNames();
                prevFieldColumn.setComboValues(prevFields);

                int nrKeys = tFilterFields.nrNonEmpty();
                for (int i = 0; i < nrKeys; i++) {
                    TableItem item = tFilterFields.getNonEmpty(i);
                    String field = item.getText(1);
                    item.setText(2, "=");
                    item.setText(3, "");
                    if (field != null && !field.equals("")) {
                        for (String prevField : prevFields) {
                            if (field.equalsIgnoreCase(prevField)) {
                                item.setText(3, field);
                                enablePreview = false;
                                break;
                            }
                        }
                    }
                }
            } else {
                prevFields = new String[0];
                int nrKeys = tFilterFields.nrNonEmpty();
                for (int i = 0; i < nrKeys; i++) {
                    TableItem item = tFilterFields.getNonEmpty(i);
                    item.setText(2, "");
                }
            }

            wPreview.setEnabled(enablePreview);
            tFilterFields.removeEmptyRows();
            tFilterFields.setRowNums();
            tFilterFields.optWidth(true);
        } catch (Exception e) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e); //$NON-NLS-1$ //$NON-NLS-2$
            logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
        }
        return true;
    }

    /*
     * Ver comentario del método ok(), no obstante los valores guardados en la
     * clase de intercambio CiviInputMeta son leídos aquí y cargados en el
     * dialogo, pues este método siempre es llamado antes de hacer visible el
     * mismo. Hay que notar que la primera vez que se ejecuta el dialogo al no
     * tener nada guardado se usan los valores establecidos en el método
     * setDefault de CiviInputMeta.
     */
    public void getData() {

        super.getData();

        wStepname.selectAll();

        if (((CiviInputMeta) input).getCiviCrmOnMultipleRows() != null) {
            wCiviCrmOnMultipleRows.setText(((CiviInputMeta) input).getCiviCrmOnMultipleRows());
        }

        if (((CiviInputMeta) input).getCiviCrmPassRowOnFail() != null) {
            wCiviCrmPassRowOnFail.setSelection(((CiviInputMeta) input).getCiviCrmPassRowOnFail());
        }

        if (((CiviInputMeta) input).getCiviCrmPageSize() != null) {
            wCiviCrmPageSize.setText(((CiviInputMeta) input).getCiviCrmPageSize().toString());
        }

        if (((CiviInputMeta) input).getCiviCrmAction() != null && ((CiviInputMeta) input).getCiviCrmAction().equals("getoptions")) {
            wCiviCrmOptionFields.setText(((CiviInputMeta) input).getCiviCrmEntityOptionField());
            wCiviCrmOptionFields.setEnabled(true);
        } else {
            wCiviCrmOptionFields.setText("");
            wCiviCrmOptionFields.setEnabled(false);
        }

        ArrayList<String> opList = ((CiviInputMeta) input).getCiviCrmFilterOperator();
        this.comboAllFieldList = new ArrayList<String>();
        if (((CiviInputMeta) input).getCiviCrmFilterMap() != null) {
            // Si hay elementos para filtrar entonces mostrarlos en la tabla
            int i = 0;
            if (((CiviInputMeta) input).getCiviCrmFilterList().size() > 0) {
                for (String cFilter : ((CiviInputMeta) input).getCiviCrmFilterList()) {
                    this.comboAllFieldList.add(cFilter);
                    TableItem item = new TableItem(tFilterFields.table, SWT.NONE);
                    item.setText(1, cFilter);
                    String op = (i >= opList.size()) ? "=" : opList.get(i);
                    item.setText(2, op);
                    String filterValue = ((CiviInputMeta) input).getCiviCrmFilterMap().get(cFilter);
                    item.setText(3, (filterValue != null) ? filterValue : "");
                    i++;
                }
            }
            Collections.sort(this.comboAllFieldList);
        }

        wCiviCrmOptionFields.setItems(this.comboAllFieldList.toArray(new String[this.comboAllFieldList.size()]));

        this.operatorFieldColumn.setComboValues(new String[] { "=", "<", ">", "LIKE", "NOT LIKE" });
        this.filterFieldsColumn.setComboValues(this.comboAllFieldList.toArray(new String[this.comboAllFieldList.size()]));

        updatePreviousFields();

        tFilterFields.removeEmptyRows();
        tFilterFields.setRowNums();
        tFilterFields.optWidth(true);
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

        int nrKeys = tOutputFields.nrNonEmpty();

        ArrayList<String> keyList = new ArrayList<String>();
        ArrayList<String> filterList = new ArrayList<String>();

        HashMap<String, String> hOutput = new HashMap<String, String>();

        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tOutputFields.getNonEmpty(i);
            // Verificamos que los elementos de salida siempre tengan un campo
            // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
            // campo

            if (item.getText(1) != null && !item.getText(1).equals("")) {
//                if (keyList.contains(item.getText(1))) {
//                    new ErrorDialog(shell,
//                            BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle"),
//                            BaseMessages.getString(PKG, "CiviCrmStep.Error.DuplicateKeyField"), new Exception());
//                    return false;
//                } else
                if (hOutput.values().contains(((item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1)))) {
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

        tFilterFields.removeEmptyRows();
        HashMap<String, String> hFilter = new HashMap<String, String>();
        ArrayList<String> fOperatorList = new ArrayList<String>();
        nrKeys = tFilterFields.nrNonEmpty();
        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tFilterFields.getNonEmpty(i);

            String fieldKey = item.getText(1);
            filterList.add(fieldKey);
            fOperatorList.add(item.getText(2).equals("") || (!"<>=NOT LIKE".contains(item.getText(2))) ? "=" : item.getText(2));
            hFilter.put(item.getText(1), item.getText(3));
        }
        stepname = wStepname.getText();

        // Datos de la conexion
        try {
            ((CiviInputMeta) input).setCiviCrmPageSize(Integer.parseInt(wCiviCrmPageSize.getText()));
        } catch (NumberFormatException e) {
            ((CiviInputMeta) input).setCiviCrmPageSize(25);
        }

        ((CiviInputMeta) input).setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
        ((CiviInputMeta) input).setCiviCrmApiKey(wCiviCrmApiKey.getText());
        ((CiviInputMeta) input).setCiviCrmSiteKey(wCiviCrmSiteKey.getText());

        // Datos de la entidad activa
        ((CiviInputMeta) input).setCiviCrmEntity(wCiviCrmEntity.getText());
        ((CiviInputMeta) input).setCiviCrmAction(wCiviCrmEntityAction.getText());
        ((CiviInputMeta) input).setCiviCrmActionList(civiCrmActionList);
        ((CiviInputMeta) input).setCiviCrmEntityList(civiCrmEntityList);
        ((CiviInputMeta) input).setCiviCrmEntityOptionField(wCiviCrmOptionFields.getEnabled() ? wCiviCrmOptionFields.getText() : "");

        // Datos para depuracion
        ((CiviInputMeta) input).setCiviCrmResultField(wCiviCrmResultField.getText());
        ((CiviInputMeta) input).setCiviCrmDebugMode(wCiviCrmDebugMode.getSelection());
        ((CiviInputMeta) input).setCiviCrmPassRowOnFail(wCiviCrmPassRowOnFail.getSelection());
        ((CiviInputMeta) input).setCiviCrmOnMultipleRows(wCiviCrmOnMultipleRows.getText());

        // Listado de campos de la entidad activa
        ((CiviInputMeta) input).setCiviCrmListingFields(civiCrmListingFields);
        ((CiviInputMeta) input).setCiviCrmFilterFields(civiCrmFilterFields);

        // Datos para los filtros a usar en las llamadas al API de CIVICRM
        ((CiviInputMeta) input).setCiviCrmFilterMap(hFilter);

        // Chequeamos si se ha desvinculado el paso del anterior, de ser así pasamos un arr
        try {
            if (transMeta.getPrevStepFields(stepname) != null && !transMeta.getPrevStepFields(stepname).isEmpty()) {
                ((CiviInputMeta) input).setCiviCrmPrevFields(new ArrayList<String>(Arrays.asList(prevFields)));
                ((CiviInputMeta) input).setHasPreviousStep(true);
            } else {
                ((CiviInputMeta) input).setCiviCrmPrevFields(new ArrayList<String>());
                ((CiviInputMeta) input).setHasPreviousStep(false);
            }
        } catch (KettleStepException e) {
            e.printStackTrace();
        }

        ((CiviInputMeta) input).setCiviCrmFilterOperator(fOperatorList);
        ((CiviInputMeta) input).setCiviCrmFilterList(filterList);

        // Datos con los campos de salida del paso
        ((CiviInputMeta) input).setCiviCrmKeyList(keyList);
        ((CiviInputMeta) input).setCiviCrmOutputMap(hOutput);

        dispose();

        return true;
    }

    protected void preview() {
        // Create the table input reader step...
        HashMap<String, String> hFilter = new HashMap<String, String>();
        ArrayList<String> fOperatorList = new ArrayList<String>();
        ArrayList<String> keyList = new ArrayList<String>();
        ArrayList<String> filterList = new ArrayList<String>();
        HashMap<String, String> hOutput = new HashMap<String, String>();

        @SuppressWarnings("unchecked")
        HashMap<String, CiviField> cloneFields = (HashMap<String, CiviField>) civiCrmListingFields.clone();


        int nrKeys = tOutputFields.nrNonEmpty();
        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tOutputFields.getNonEmpty(i);
            // Verificamos que los elementos de salida siempre tengan un campo
            // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
            // campo

            if (item.getText(1) != null && !item.getText(1).equals("")) {
                String fieldKey = item.getText(1);
                // Actualizando la lista de campos, cuando un campo no exista en la lista 
                // entonces se crea y su tipo predeterminado es String (2) 
                if (!cloneFields.containsKey(fieldKey)) {
                    CiviField field = new CiviField();
                    field.setFieldName(fieldKey);
                    field.setType((long)CiviField.valueType.get("String"));
                    cloneFields.put(fieldKey, field);
                }

                keyList.add(item.getText(1));
                hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
            }
        }

        nrKeys = tFilterFields.nrNonEmpty();
        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tFilterFields.getNonEmpty(i);

            String fieldKey = item.getText(1);

            filterList.add(fieldKey);
            fOperatorList.add(item.getText(2).equals("") || (!"<>=NOT LIKE".contains(item.getText(2))) ? "=" : item.getText(2));

            try {
                hFilter.put(item.getText(1), item.getText(3).toLowerCase().contains("like") ? URLEncoder.encode(item.getText(3), "UTF-8") : item.getText(3));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        CiviInputMeta inputMeta = new CiviInputMeta();
        inputMeta.setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
        inputMeta.setCiviCrmApiKey(wCiviCrmApiKey.getText());
        inputMeta.setCiviCrmSiteKey(wCiviCrmSiteKey.getText());
        inputMeta.setCiviCrmEntity(wCiviCrmEntity.getText());
        inputMeta.setCiviCrmAction(wCiviCrmEntityAction.getText());
        inputMeta.setCiviCrmResultField(wCiviCrmResultField.getText());
        inputMeta.setCiviCrmDebugMode(wCiviCrmDebugMode.getSelection());
        inputMeta.setCiviCrmEntityOptionField(wCiviCrmOptionFields.getText());
        inputMeta.setCiviCrmPassRowOnFail(wCiviCrmPassRowOnFail.getSelection());
        inputMeta.setCiviCrmOnMultipleRows(wCiviCrmOnMultipleRows.getText());

        inputMeta.setCiviCrmListingFields(cloneFields);
        inputMeta.setCiviCrmKeyList(keyList);
        inputMeta.setCiviCrmFilterList(filterList);
        inputMeta.setCiviCrmFilterOperator(fOperatorList);
        inputMeta.setCiviCrmOutputMap(hOutput);
        inputMeta.setCiviCrmFilterMap(hFilter);

        try {
            TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(transMeta, inputMeta, wStepname.getText());

            EnterNumberDialog numberDialog = new EnterNumberDialog(shell, (((wCiviCrmPageSize.getText() != null) && (wCiviCrmPageSize.getText().length() > 0)) ? Integer.parseInt(wCiviCrmPageSize.getText()): 25), BaseMessages.getString(PKG,
                    "CiviCrmDialog.Preview.Title"), BaseMessages.getString(PKG, "CiviCrmDialog.Preview.NumberOfRowsToPreview")); //$NON-NLS-1$ //$NON-NLS-2$
            int previewSize = numberDialog.open();
            if (previewSize > 0) {
                inputMeta.setCiviCrmPageSize(previewSize);
                TransPreviewProgressDialog progressDialog = new TransPreviewProgressDialog(shell, previewMeta,
                        new String[] { wStepname.getText() }, new int[] { previewSize });
                progressDialog.open();

                Trans trans = progressDialog.getTrans();
                String loggingText = progressDialog.getLoggingText();

                if (!progressDialog.isCancelled()) {
                    if (trans.getResult() != null && trans.getResult().getNrErrors() > 0) {
                        EnterTextDialog etd = new EnterTextDialog(shell, BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title"),
                                BaseMessages.getString(PKG, "System.Dialog.PreviewError.Message"), loggingText, true);
                        etd.setReadOnly();
                        etd.open();
                    } else {
                        PreviewRowsDialog prd = new PreviewRowsDialog(shell, transMeta, SWT.NONE, wStepname.getText(),
                                progressDialog.getPreviewRowsMeta(wStepname.getText()), progressDialog.getPreviewRows(wStepname.getText()), loggingText);
                        prd.open();
                    }
                }

            }
        } catch (Exception e) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "System.Dialog.PreviewError.Title"), e.toString().split(":")[0], e);
        }
    }

}
