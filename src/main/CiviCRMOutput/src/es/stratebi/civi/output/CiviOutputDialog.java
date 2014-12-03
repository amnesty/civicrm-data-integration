package es.stratebi.civi.output;

import es.stratebi.civi.CiviDialog;
import es.stratebi.civi.util.CiviField;
import es.stratebi.civi.util.CiviRestService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.row.RowMetaInterface;
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

import java.util.*;
import java.util.List;

public class CiviOutputDialog extends CiviDialog implements StepDialogInterface {
    // connection settings widgets
    private Label wlCiviCrmRestUrl;
    private Label wlCiviCrmApiKey;
    private Label wlCiviCrmSiteKey;
    private Label wlCiviCrmEntity;

    private Group gConnectionGroup;

    // all fields from the previous steps, used for drop down selection
    private RowMetaInterface prevFields = null;

    // the drop down column which should contain previous fields from stream
    private ColumnInfo streamFieldColumn = null;

    private Group gEntity;

    private CTabFolder wTabFolder;

    private CTabItem wOutputFieldsTab;

    private Composite gOutputFields;

    private Button wGetEntities;

    private Listener lsGetEntities;
    private Label wlCiviCrmDebugMode;
    private int margin;
    private int middle;
    private ModifyListener lsMod;
    private Button wEntityListBtn;
    private Label wlCiviCrmEntityAction;
    private CTabFolder wConnectionFolder;
    private CTabItem wConnectionTab;
    private Composite gConnectionFields;
    private CTabItem wPerfomanceTab;
    private Composite gPerfomanceFields;
    private Label wlCiviCrmPageSize;
    private TextVar wCiviCrmPageSize;
    private Label wlCiviCrmResultField;


    // constructor
    public CiviOutputDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
        super(parent, (BaseStepMeta) in, transMeta, sname);
        input = (CiviOutputMeta) in;
    }

    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        props.setLook(shell);
        setShellImage(shell, (CiviOutputMeta) input);

        lsMod = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                ((CiviOutputMeta) input).setChanged();
            }
        };
        backupChanged = ((CiviOutputMeta) input).hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "CiviCrmDialog.Shell.Output.Title"));

        middle = props.getMiddlePct();
        margin = Const.MARGIN;

        /*************************************************
         * STEP NAME ENTRY
         *************************************************/

        // Stepname line
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

        addOkCancelButtons();
        addConnectionTab();
        addEntityGroup();
        addOutputTab();

        // wGetInputFields.addListener(SWT.Selection, lsGetInputFields);
        /*************************************************
         * // DEFAULT ACTION LISTENERS
         *************************************************/

        lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };
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
        ((CiviOutputMeta) input).setChanged(backupChanged);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        return stepname;
    }

    private void addConnectionTab() {
        wConnectionFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(wConnectionFolder, Props.WIDGET_STYLE_TAB);

        /*************************************************
         * // CIVICRM CONNECTION TAB
         *************************************************/

        wConnectionTab = new CTabItem(wConnectionFolder, SWT.NONE);
        wConnectionTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ConnectionTab.Label"));

        gConnectionFields = new Composite(wConnectionFolder, SWT.NONE);
        props.setLook(gConnectionFields);

        FormLayout connectionFieldsCompLayout = new FormLayout();
        connectionFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        connectionFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gConnectionFields.setLayout(connectionFieldsCompLayout);

        /************************************************************************************/
        // CiviCrm RestUrl
        wlCiviCrmRestUrl = new Label(gConnectionFields, SWT.RIGHT);
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
        wlCiviCrmSiteKey = new Label(gConnectionFields, SWT.RIGHT);
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
        wlCiviCrmApiKey = new Label(gConnectionFields, SWT.RIGHT);
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

        wPerfomanceTab = new CTabItem(wConnectionFolder, SWT.NONE);
        wPerfomanceTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.PerfomanceTab.Label"));

        gPerfomanceFields = new Composite(wConnectionFolder, SWT.NONE);
        props.setLook(gPerfomanceFields);

        FormLayout perfomanceFieldsCompLayout = new FormLayout();
        connectionFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        connectionFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gPerfomanceFields.setLayout(perfomanceFieldsCompLayout);

        /****************************************************************************/
        // CiviCrm pageSize label
        wlCiviCrmPageSize = new Label(gPerfomanceFields, SWT.RIGHT);
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
        wlCiviCrmResultField = new Label(gPerfomanceFields, SWT.RIGHT);
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

        // CiviCrm debug mode label
        wlCiviCrmDebugMode = new Label(gPerfomanceFields, SWT.RIGHT);
        wlCiviCrmDebugMode.setText(BaseMessages.getString(PKG, "CiviCrmDialog.DebugMode.Label"));
        props.setLook(wlCiviCrmDebugMode);

        FormData fdlDebugMode = new FormData();
        fdlDebugMode.top = new FormAttachment(wCiviCrmResultField, margin);
        fdlDebugMode.left = new FormAttachment(0, 0);
        fdlDebugMode.right = new FormAttachment(middle, -margin);
        wlCiviCrmDebugMode.setLayoutData(fdlDebugMode);

        // CiviCrm debug mode checkbox
        wCiviCrmDebugMode = new Button(gPerfomanceFields, SWT.CHECK);
        wCiviCrmDebugMode.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.DebugMode.Tooltip"));
        props.setLook(wCiviCrmDebugMode);

        FormData fdCiviCrmdDebugMode = new FormData();
        fdCiviCrmdDebugMode.right = new FormAttachment(wlCiviCrmDebugMode, 25, SWT.RIGHT);
        fdCiviCrmdDebugMode.top = new FormAttachment(wCiviCrmResultField, margin);
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
        fdConnectionFolder.left = new FormAttachment(0, 0);
        fdConnectionFolder.top = new FormAttachment(wStepname, margin);
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

        lsGetEntities = new Listener() {
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
        wlCiviCrmEntity = new Label(gEntity, SWT.RIGHT);
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
                ((CiviOutputMeta) input).setChanged();

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
        wlCiviCrmEntityAction = new Label(gEntity, SWT.RIGHT);
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
    }

    private void addOutputTab() {
        wTabFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

        /*************************************************
         * // CIVICRM OUTPUT TAB
         *************************************************/

        wOutputFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
        wOutputFieldsTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.OutputFieldsGroup.Title")); //$NON-NLS-1$

        gOutputFields = new Composite(wTabFolder, SWT.NONE);
        props.setLook(gOutputFields);

        FormLayout outputFieldsCompLayout = new FormLayout();
        outputFieldsCompLayout.marginWidth = Const.FORM_MARGIN;
        outputFieldsCompLayout.marginHeight = Const.FORM_MARGIN;
        gOutputFields.setLayout(outputFieldsCompLayout);

        // -----------------------------------------------
        /*************************************************
         * // KEY / OUTPUT TABLE
         *************************************************/

        int outputKeyWidgetCols = 3;
        int outputKeyWidgetRows = (((CiviOutputMeta) input).getCiviCrmListingFields() != null ? ((CiviOutputMeta) input).getCiviCrmOutputMap().size()
                : 3);

        ColumnInfo[] ciFields = new ColumnInfo[outputKeyWidgetCols];
        streamFieldColumn = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.StreamField"), ColumnInfo.COLUMN_TYPE_CCOMBO,
                new String[]{}, false);
        outputFieldsColumn = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.EntityField"), ColumnInfo.COLUMN_TYPE_CCOMBO,
                new String[]{}, false);
        ciFields[0] = streamFieldColumn;
        ciFields[1] = outputFieldsColumn;
//        ciFields[2] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.Source"), ColumnInfo.COLUMN_TYPE_NONE,false);
        ciFields[2] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.TitleField"), ColumnInfo.COLUMN_TYPE_NONE,false);

        tOutputFields = new TableView(transMeta, gOutputFields, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
                ciFields, outputKeyWidgetRows, lsMod, props);

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

        tOutputFields.setLayoutData(fdOutputFields);

        gOutputFields.layout();

        wOutputFieldsTab.setControl(gOutputFields);


        FormData fdTabFolder = new FormData();
        fdTabFolder.left = new FormAttachment(0, 0);
        fdTabFolder.top = new FormAttachment(gEntity, margin);
        fdTabFolder.right = new FormAttachment(100, 0);
        fdTabFolder.bottom = new FormAttachment(wOK, -margin);
        wTabFolder.setLayoutData(fdTabFolder);

        wTabFolder.setSelection(0);
    }

    private void addOkCancelButtons() {
        /*************************************************
         * // OK AND CANCEL BUTTONS
         *************************************************/

        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

        BaseStepDialog.positionBottomButtons(shell, new Button[]{wOK, wCancel}, margin, null);


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

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);
    }

    // Collect data from the meta and place it in the dialog
    public void getData() {
        super.getData();

    /*
     * Si no hay campos de salida entonces automaticamente todos los campos que
     * vienen del paso anterior. Es responsabilidad del usuario si hace
     * modificaciones en los campos que llegan a este paso actualizar nuevamente
     * el listado de campo de salida
     */
        if ((((CiviOutputMeta) input).getCiviCrmOutputMap() != null) && (((CiviOutputMeta) input).getCiviCrmOutputMap().size() > 0)) {
            // Si hay elementos para filtrar entonces mostrarlos en la tabla
            int i = 0;
            String[] streamFields = new String[((CiviOutputMeta) input).getCiviCrmOutputMap().size()];
            for (String cField : ((CiviOutputMeta) input).getCiviCrmKeyList()) {
                TableItem item = tOutputFields.table.getItem(i);
                item.setText(1, cField);
                item.setText(2, ((CiviOutputMeta) input).getCiviCrmOutputMap().get(cField));
                streamFields[i++] = cField;
            }

            this.streamFieldColumn.setComboValues(streamFields);
        }

        tOutputFields.optWidth(true);
        tOutputFields.removeEmptyRows();
        tOutputFields.setRowNums();
    }

    /*
     * Aqui se realiza una llamada al API de CIVICRM y se obtiene el listado de
     * campos que admite la entidad, los cuales son desplegados en las tablas que
     * muestran los campos y filtros
     */
    protected boolean getEntityFieldsAndActions() {
        try {
            if (((CiviOutputMeta) input).getCiviCrmListingFields().size() > 0) {
                MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
                boolean goOn = MessageDialog.openConfirm(shell, BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Title"),
                        BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Msg"));
                if (!goOn) {
                    return false;
                }
            }

            // Eliminamos los campos de salida y volvemos a actualizar la tabla con
            // los campos de entrada mas los de la entidad seleccionada mapeando los
            // que se llamen igual
            tOutputFields.clearAll();
            tOutputFields.removeAll();

            if (prevFields != null) {
                prevFields.clear();
            }

            activeEntity = wCiviCrmEntity.getText();

            //Obtenemos y llenamos la tabla con los campos que llegan del paso anterior
            prevFields = transMeta.getPrevStepFields(stepname);
            if (prevFields != null && !prevFields.isEmpty()) {
                BaseStepDialog.getFieldsFromPrevious(prevFields, tOutputFields, 1, new int[]{1}, new int[]{}, -1, -1, null);
                streamFieldColumn.setComboValues(prevFields.getFieldNames());
            }

            String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
            String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
            String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());

            CiviRestService crUtil = new CiviRestService(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

            String[] comboValues = new String[0];

            this.comboFieldList.clear();
            civiCrmFilterFields.clear();
            civiCrmListingFields.clear();

            civiCrmListingFields =  crUtil.getFieldList(true);

            for (CiviField field : civiCrmListingFields.values()) {
                this.comboFieldList.add(field.getFieldName());
            }
            Collections.sort(this.comboFieldList);

            this.outputFieldsColumn.setComboValues(this.comboFieldList.toArray(new String[0]));

            // Verificamos que los elementos de salida siempre tengan un campo válido
            // de CIVICRM asignado si lo tiene, en caso de no tenerlo se busca y se
            // asigna si existe. Si lo tiene y no existe entonces se reemplaza por
            // el campo asociado si lo hubiera con igual nombre
            int nrKeys = tOutputFields.nrNonEmpty();
            for (int i = 0; i < nrKeys; i++) {
                TableItem item = tOutputFields.getNonEmpty(i);
                String streamField = item.getText(1);
                if (streamField != null && !streamField.equals("")) {
                    CiviField field = civiCrmListingFields.get(streamField);
                    if (field != null) {
                        item.setText(2, field.getFieldName());
                        item.setText(3, field.getTitle());
                    } else {
                        item.setText(2, "");
                        item.setText(3, "");
                    }
                }
            }

            tOutputFields.removeEmptyRows();
            tOutputFields.setRowNums();
            tOutputFields.optWidth(true);

            crUtil.setAction("getactions");
            civiCrmActionList = crUtil.getEntityActions(false);

            String[] eArray = (String[]) civiCrmActionList.toArray(new String[0]);
            wCiviCrmEntityAction.setItems(eArray);
            String action = eArray.length > 0 ? (civiCrmActionList.contains("create") ? "create" : "") : eArray[0];
            wCiviCrmEntityAction.setText(action);
        } catch (Exception e) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e); //$NON-NLS-1$ //$NON-NLS-2$
            logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
        }
        return true;
    }

    protected boolean ok() {
        int nrKeys = tOutputFields.nrNonEmpty();

        //HashMap<String, CiviField> fields = ((CiviMeta) input).getCiviCrmListingFields();
        ArrayList<String> keyList = new ArrayList<String>();
        List<String> streamFields = Arrays.asList(streamFieldColumn.getComboValues());

        HashMap<String, String> hOutput = new HashMap<String, String>();

        for (int i = 0; i < nrKeys; i++) {
            TableItem item = tOutputFields.getNonEmpty(i);
            // Verificamos que los elementos de salida siempre tengan un campo
            // seleccionado y luego si no hay un alias le ponemos el mismo nombre
            // del campo

//            if (keyList.contains(item.getText(1))) {
//                new ErrorDialog(shell,
//                        BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle"),
//                        BaseMessages.getString(PKG, "CiviCrmStep.Error.DuplicateKeyField"), new Exception());
//                return false;
//            } else
            if (hOutput.containsKey((item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1))) {
                new ErrorDialog(shell,
                        BaseMessages.getString(PKG, "CiviCrmStep.Error.ErrorTitle"),
                        BaseMessages.getString(PKG, "CiviCrmStep.Error.DuplicateOutputField"), new Exception());
                return false;
            } else {
                keyList.add(item.getText(1));
                hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
            }
        }

        stepname = wStepname.getText();

        // Datos de la conexion
        ((CiviOutputMeta) input).setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
        ((CiviOutputMeta) input).setCiviCrmApiKey(wCiviCrmApiKey.getText());
        ((CiviOutputMeta) input).setCiviCrmSiteKey(wCiviCrmSiteKey.getText());

        // Datos para depuracion
        ((CiviOutputMeta) input).setCiviCrmDebugMode(wCiviCrmDebugMode.getSelection());
        ((CiviOutputMeta) input).setCiviCrmResultField(wCiviCrmResultField.getText());

        // Datos de la entidad activa
        ((CiviOutputMeta) input).setCiviCrmEntity(wCiviCrmEntity.getText());
        ((CiviOutputMeta) input).setCiviCrmAction(wCiviCrmEntityAction.getText());
        ((CiviOutputMeta) input).setCiviCrmActionList(civiCrmActionList);

        // Listado de campos de la entidad activa
        ((CiviOutputMeta) input).setCiviCrmListingFields(civiCrmListingFields);

        // Datos con los campos de salida del paso
        ((CiviOutputMeta) input).setCiviCrmKeyList(keyList);
        ((CiviOutputMeta) input).setCiviCrmOutputMap(hOutput);

        dispose();
        return true;
    }

    protected void updatePreviousFields() {
        try {
            prevFields = transMeta.getPrevStepFields(stepname);

            if (prevFields != null && !prevFields.isEmpty()) {
                streamFieldColumn.setComboValues(prevFields.getFieldNames());
        /*
         * int nrKeys = tFilterFields.nrNonEmpty(); for (int i = 0; i < nrKeys;
         * i++) { TableItem item = tFilterFields.getNonEmpty(i); String field =
         * item.getText(1); item.setText(3, ""); if (field != null &&
         * !field.equals("")) { for (int t = 0; t < prevFields.length; t++) { if
         * (field.equalsIgnoreCase(prevFields[t])) { item.setText(3, field);
         * break; } } } } } else { prevFields = null; int nrKeys =
         * tFilterFields.nrNonEmpty(); for (int i = 0; i < nrKeys; i++) {
         * TableItem item = tFilterFields.getNonEmpty(i); item.setText(3, ""); }
         */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
