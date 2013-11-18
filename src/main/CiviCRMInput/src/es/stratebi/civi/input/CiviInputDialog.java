package es.stratebi.civi.input;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
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
import es.stratebi.civi.CiviMeta;
import es.stratebi.civi.util.FieldAttrs;
import es.stratebi.civi.util.RestUtil;

/*
 * 
 * 
 * Este es el dialogo de conexion a la API REST de CiviCRM
 */

public class CiviInputDialog extends CiviDialog implements StepDialogInterface {
  // connection settings widgets
  private Label wlCiviCrmRestUrl;
  private Label wlCiviCrmApiKey;
  private Label wlCiviCrmSiteKey;
  private Label wlCiviCrmEntity;
  private Label wlCiviCrmPageSize;

  private TextVar wCiviCrmPageSize;

  private Group gConnectionGroup;

  // lookup fields settings widgets
  public TableView tFilterFields;

  private ColumnInfo filterFieldsColumn = null;

  private Group gEntity;
  private Composite gEntityFilter;
  private Composite gOutputFields;

  private CTabFolder wTabFolder;
  private CTabItem wFilterFieldsTab;
  private CTabItem wOutputFieldsTab;

  private Button wGetEntities;
  private Button wEntityListBtn;
  private Button wTestConnection;

  private Listener lsGetEntities;
  private Listener lsTestConnection;

  // Constructor
  public CiviInputDialog(Shell parent, Object in, TransMeta transMeta, String sname) {
    super(parent, (BaseStepMeta) in, transMeta, sname);
  }

  // Construye y muestra el diálogo
  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    props.setLook(shell);
    setShellImage(shell, (StepMetaInterface) input);

    ModifyListener lsMod = new ModifyListener() {
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

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

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

    /*************************************************
     * // CIVICRM CONNECTION GROUP
     *************************************************/
    gConnectionGroup = new Group(shell, SWT.NONE);
    gConnectionGroup.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ConnectGroup.Label"));
    props.setLook(gConnectionGroup);

    FormLayout frmDataLayout = new FormLayout();
    frmDataLayout.marginWidth = 3;
    frmDataLayout.marginHeight = 3;
    gConnectionGroup.setLayout(frmDataLayout);

    FormData fdComposite = new FormData();
    fdComposite.left = new FormAttachment(0, 0);
    fdComposite.right = new FormAttachment(100, 0);
    fdComposite.top = new FormAttachment(wStepname, margin);
    gConnectionGroup.setLayoutData(fdComposite);

    // ---------------------------------------------

    // CiviCrm RestUrl
    wlCiviCrmRestUrl = new Label(gConnectionGroup, SWT.RIGHT);
    wlCiviCrmRestUrl.setText(BaseMessages.getString(PKG, "CiviCrmDialog.RestURL.Label"));
    props.setLook(wlCiviCrmRestUrl);

    FormData fdlCiviCrmRestUrl = new FormData();
    fdlCiviCrmRestUrl.top = new FormAttachment(0, margin);
    fdlCiviCrmRestUrl.left = new FormAttachment(0, 0);
    fdlCiviCrmRestUrl.right = new FormAttachment(middle, -margin);
    wlCiviCrmRestUrl.setLayoutData(fdlCiviCrmRestUrl);

    wCiviCrmRestUrl = new TextVar(transMeta, gConnectionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wCiviCrmRestUrl.addModifyListener(lsMod);
    wCiviCrmRestUrl.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.RestURL.Tooltip"));
    props.setLook(wCiviCrmRestUrl);

    FormData fdCiviCrmRestUrl = new FormData();
    fdCiviCrmRestUrl.top = new FormAttachment(0, margin);
    fdCiviCrmRestUrl.left = new FormAttachment(middle, 0);
    fdCiviCrmRestUrl.right = new FormAttachment(100, 0);
    wCiviCrmRestUrl.setLayoutData(fdCiviCrmRestUrl);

    // CiviCrm SiteKey
    wlCiviCrmSiteKey = new Label(gConnectionGroup, SWT.RIGHT);
    wlCiviCrmSiteKey.setText(BaseMessages.getString(PKG, "CiviCrmDialog.SiteKey.Label"));
    props.setLook(wlCiviCrmSiteKey);

    FormData fdlCiviCrmSiteKey = new FormData();
    fdlCiviCrmSiteKey.top = new FormAttachment(wCiviCrmRestUrl, margin);
    fdlCiviCrmSiteKey.left = new FormAttachment(0, 0);
    fdlCiviCrmSiteKey.right = new FormAttachment(middle, -margin);
    wlCiviCrmSiteKey.setLayoutData(fdlCiviCrmSiteKey);

    wCiviCrmSiteKey = new TextVar(transMeta, gConnectionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wCiviCrmSiteKey.addModifyListener(lsMod);
    wCiviCrmSiteKey.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.SiteKey.Tooltip"));
    props.setLook(wCiviCrmSiteKey);

    FormData fdCiviCrmSiteKey = new FormData();
    fdCiviCrmSiteKey.top = new FormAttachment(wCiviCrmRestUrl, margin);
    fdCiviCrmSiteKey.left = new FormAttachment(middle, 0);
    fdCiviCrmSiteKey.right = new FormAttachment(100, 0);
    wCiviCrmSiteKey.setLayoutData(fdCiviCrmSiteKey);

    // CiviCrm ApiKey
    wlCiviCrmApiKey = new Label(gConnectionGroup, SWT.RIGHT);
    wlCiviCrmApiKey.setText(BaseMessages.getString(PKG, "CiviCrmDialog.ApiKey.Label"));
    props.setLook(wlCiviCrmApiKey);

    FormData fdlCiviCrmApiKey = new FormData();
    fdlCiviCrmApiKey.top = new FormAttachment(wCiviCrmSiteKey, margin);
    fdlCiviCrmApiKey.left = new FormAttachment(0, 0);
    fdlCiviCrmApiKey.right = new FormAttachment(middle, -margin);
    wlCiviCrmApiKey.setLayoutData(fdlCiviCrmApiKey);

    wCiviCrmApiKey = new TextVar(transMeta, gConnectionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wCiviCrmApiKey.addModifyListener(lsMod);
    wCiviCrmApiKey.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.ApiKey.Tooltip"));
    props.setLook(wCiviCrmApiKey);

    FormData fdCiviCrmApiKey = new FormData();
    fdCiviCrmApiKey.top = new FormAttachment(wCiviCrmSiteKey, margin);
    fdCiviCrmApiKey.left = new FormAttachment(middle, 0);
    fdCiviCrmApiKey.right = new FormAttachment(100, 0);
    wCiviCrmApiKey.setLayoutData(fdCiviCrmApiKey);

    // CiviCrm pageSize label
    wlCiviCrmPageSize = new Label(gConnectionGroup, SWT.RIGHT);
    wlCiviCrmPageSize.setText(BaseMessages.getString(PKG, "CiviCrmDialog.PageSize.Label"));
    props.setLook(wlCiviCrmPageSize);

    FormData fdlCiviPageSize = new FormData();
    fdlCiviPageSize.top = new FormAttachment(wCiviCrmApiKey, margin);
    fdlCiviPageSize.left = new FormAttachment(0, 0);
    fdlCiviPageSize.right = new FormAttachment(middle, -margin);
    wlCiviCrmPageSize.setLayoutData(fdlCiviPageSize);
    
    // CiviCrm pageSize text
    wCiviCrmPageSize = new TextVar(transMeta, gConnectionGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wCiviCrmPageSize.addModifyListener(lsMod);
    wCiviCrmPageSize.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.PageSize.Tooltip"));
    props.setLook(wCiviCrmPageSize);

    FormData fdCiviPageSize = new FormData();
    fdCiviPageSize.top = new FormAttachment(wCiviCrmApiKey, margin);
    fdCiviPageSize.left = new FormAttachment(middle, 0);
    fdCiviPageSize.right = new FormAttachment(100, 0);
    wCiviCrmPageSize.setLayoutData(fdCiviPageSize);
    
    // Test connection button
    wTestConnection = new Button(gConnectionGroup, SWT.PUSH);
    wTestConnection.setText(BaseMessages.getString(PKG, "CiviCrmDialog.TestConnection.Button")); //$NON-NLS-1$

    // Get entity fields button
    wGetEntities = new Button(gConnectionGroup, SWT.PUSH);
    wGetEntities.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityList.Button")); //$NON-NLS-1$

    FormData fdGetEntities = new FormData();
    fdGetEntities.top = new FormAttachment(wCiviCrmPageSize, margin);
    fdGetEntities.right = new FormAttachment(100, 0);
    wGetEntities.setLayoutData(fdGetEntities);

    FormData fdTestConnection = new FormData();
    fdTestConnection.top = new FormAttachment(wCiviCrmPageSize, margin);
    fdTestConnection.right = new FormAttachment(wGetEntities, -margin);
    wTestConnection.setLayoutData(fdTestConnection);

    lsTestConnection = new Listener() {
      public void handleEvent(Event e) {
        testConnection();
      }
    };

    wTestConnection.addListener(SWT.Selection, lsTestConnection);

    lsGetEntities = new Listener() {
      public void handleEvent(Event e) {
        getEntities();
      }
    };

    wGetEntities.addListener(SWT.Selection, lsGetEntities);

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
    fdEntity.top = new FormAttachment(gConnectionGroup, margin);
    gEntity.setLayoutData(fdEntity);

    // -----------------------------------------------

    // CiviCrm Entity name
    wlCiviCrmEntity = new Label(gEntity, SWT.RIGHT);
    wlCiviCrmEntity.setText(BaseMessages.getString(PKG, "CiviCrmDialog.Entity.Label"));
    props.setLook(wlCiviCrmEntity);

    FormData fdlCiviCrmEntity = new FormData();
    fdlCiviCrmEntity.top = new FormAttachment(gEntity, margin);
    fdlCiviCrmEntity.left = new FormAttachment(0, 0);
    fdlCiviCrmEntity.right = new FormAttachment(middle, -margin);
    wlCiviCrmEntity.setLayoutData(fdlCiviCrmEntity);

    wCiviCrmEntity = new CCombo(gEntity, SWT.BORDER);
    wCiviCrmEntity.addModifyListener(lsMod);
    wCiviCrmEntity.setToolTipText(BaseMessages.getString(PKG, "CiviCrmDialog.Entity.Tooltip"));
    props.setLook(wCiviCrmEntity);

    // Get entity fields button
    wEntityListBtn = new Button(gEntity, SWT.PUSH);
    wEntityListBtn.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityFields.Button")); //$NON-NLS-1$
    FormData fdGetFields = new FormData();
    fdGetFields.top = new FormAttachment(gEntity, margin);
    fdGetFields.right = new FormAttachment(100, 0);
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
        getEntityFields();
      }
    };

    wEntityListBtn.addListener(SWT.Selection, lsGetFields);

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

    int outputKeyWidgetCols = 2;
    int outputKeyWidgetRows = (((CiviInputMeta) input).getFields() != null ? ((CiviInputMeta) input).getFields().values().size() : 3);

    ColumnInfo[] ciOutputKeys = new ColumnInfo[outputKeyWidgetCols];
    ciOutputKeys[0] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldName"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        new String[] {}, false);
    ciOutputKeys[1] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldRename"), ColumnInfo.COLUMN_TYPE_TEXT,
        false);
    
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
    wFilterFieldsTab = new CTabItem(wTabFolder, SWT.NONE);
    wFilterFieldsTab.setText(BaseMessages.getString(PKG, "CiviCrmDialog.FilterGroup.Title")); //$NON-NLS-1$

    gEntityFilter = new Composite(wTabFolder, SWT.NONE);
    props.setLook(gEntityFilter);

    FormLayout fieldsCompLayout = new FormLayout();
    fieldsCompLayout.marginWidth = Const.FORM_MARGIN;
    fieldsCompLayout.marginHeight = Const.FORM_MARGIN;
    gEntityFilter.setLayout(fieldsCompLayout);

    /*************************************************
     * // KEY / FILTER TABLE
     *************************************************/
    int keyWidgetCols = 2;
    int keyWidgetRows = (((CiviInputMeta) input).getFields() != null ? ((CiviInputMeta) input).getFields().values().size() : 1);

    ColumnInfo[] ciKeys = new ColumnInfo[keyWidgetCols];
    ciKeys[0] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldName"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        new String[] {}, false);
    ciKeys[1] = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldValue"), ColumnInfo.COLUMN_TYPE_TEXT, false);

    filterFieldsColumn = ciKeys[0];

    tFilterFields = new TableView(transMeta, gEntityFilter, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
        ciKeys, keyWidgetRows, lsMod, props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(0, margin);
    fdFields.right = new FormAttachment(100, -margin);
    fdFields.bottom = new FormAttachment(100, -margin);
    tFilterFields.setLayoutData(fdFields);

    FormData fdFilterFieldsComp = new FormData();
    fdFilterFieldsComp.left = new FormAttachment(0, 0);
    fdFilterFieldsComp.top = new FormAttachment(0, 0);
    fdFilterFieldsComp.right = new FormAttachment(100, 0);
    fdFilterFieldsComp.bottom = new FormAttachment(100, 0);
    gEntityFilter.setLayoutData(fdFilterFieldsComp);

    gEntityFilter.layout();
    wFilterFieldsTab.setControl(gEntityFilter);

    /*************************************************
     * // OK AND CANCEL BUTTONS
     *************************************************/

    wOK = new Button(shell, SWT.PUSH);
    wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wPreview = new Button(shell, SWT.PUSH);
    wPreview.setText(BaseMessages.getString(PKG, "System.Button.Preview")); //$NON-NLS-1$
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(gEntity, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOK, -margin);
    wTabFolder.setLayoutData(fdTabFolder);

    BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wPreview, wCancel }, margin, null);

    wTabFolder.setSelection(0);
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

  protected void getEntityFields() {
    super.getEntityFields();
    tFilterFields.removeAll();
    tFilterFields.setRowNums();
    tFilterFields.optWidth(true);
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

    wStepname.selectAll();
    super.getData();

    if (((CiviInputMeta) input).getCiviCrmPageSize() != null) {
      wCiviCrmPageSize.setText(((CiviInputMeta) input).getCiviCrmPageSize().toString());
    }

    if (((CiviInputMeta) input).getFields() != null) {

      if (((CiviInputMeta) input).getFilterMap() != null) {
        // Si hay elementos para filtrar entonces mostrarlos en la tabla
        int i = 0;
        for (String cFilter : ((CiviInputMeta) input).getFilterList()) {
          TableItem item = tFilterFields.table.getItem(i);
          item.setText(1, cFilter);
          item.setText(2, ((CiviInputMeta) input).getFilterMap().get(cFilter));
          i++;
        }
      }
    }

    this.filterFieldsColumn.setComboValues(this.comboFieldList);

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
    boolean matchFields = true;

    HashMap<String, FieldAttrs> fields = ((CiviMeta) input).getFields();
    ArrayList<String> keyList = new ArrayList<String>();
    ArrayList<String> filterList = new ArrayList<String>();

    HashMap<String, String> hOutput = new HashMap<String, String>();

    for (int i = 0; i < nrKeys; i++) {
      TableItem item = tOutputFields.getNonEmpty(i);
      // Verificamos que los elementos de salida siempre tengan un campo
      // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
      // campo
      
      if (item.getText(1) != null && !item.getText(1).equals("")) {
        String fieldKey = item.getText(1); 
        matchFields = (fields.get(fieldKey) != null);
        if (!matchFields) {
          String msg = "";
             msg = BaseMessages.getString(PKG, "CiviCrmStep.Error.FieldNotMatch")
             .replace("$1", fieldKey)
             .replace("$2", activeEntity);
          Exception e = new Exception();
          new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.UnableFindField"), msg, e);
          break;
        }

        keyList.add(item.getText(1));
        hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
      }
    }

    HashMap<String, String> hFilter = new HashMap<String, String> ();
    if (matchFields) {
      nrKeys = tFilterFields.nrNonEmpty();
      for (int i = 0; i < nrKeys; i++) {
        TableItem item = tFilterFields.getNonEmpty(i);

        String fieldKey = item.getText(1); 
        matchFields = (fields.get(fieldKey) != null);
        
        if (!matchFields) {
          String msg = "";
             msg = BaseMessages.getString(PKG, "CiviCrmStep.Error.FieldNotMatch")
             .replace("$1", fieldKey)
             .replace("$2", activeEntity);
          Exception e = new Exception();
          new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.UnableFindField"), msg, e);
          break;
        }

        filterList.add(fieldKey);
        hFilter.put(item.getText(1), item.getText(2));
      }
    }
    if (matchFields) {
      stepname = wStepname.getText();

      ((CiviInputMeta) input).setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
      ((CiviInputMeta) input).setCiviCrmApiKey(wCiviCrmApiKey.getText());
      ((CiviInputMeta) input).setCiviCrmSiteKey(wCiviCrmSiteKey.getText());
      ((CiviInputMeta) input).setCiviCrmEntity(wCiviCrmEntity.getText());

      try {
        ((CiviInputMeta)input).setCiviCrmPageSize(Integer.parseInt(wCiviCrmPageSize.getText()));
      } catch (NumberFormatException e) {
        ((CiviInputMeta)input).setCiviCrmPageSize(25);
      }

      ((CiviInputMeta) input).setFilterList(filterList);
      ((CiviInputMeta) input).setKeyList(keyList);
      ((CiviInputMeta) input).setOutputMap(hOutput);
      ((CiviInputMeta)input).setFilterMap(hFilter);
      dispose();
      return true;
    }
    
    return false;
  }

  protected void preview() {
    // Create the table input reader step...
    CiviInputMeta inputMeta = new CiviInputMeta();
    inputMeta.setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
    inputMeta.setCiviCrmApiKey(wCiviCrmApiKey.getText());
    inputMeta.setCiviCrmSiteKey(wCiviCrmSiteKey.getText());
    inputMeta.setCiviCrmEntity(wCiviCrmEntity.getText());
    try {
      inputMeta.setCiviCrmPageSize(Integer.parseInt(wCiviCrmPageSize.getText()));
    } catch (NumberFormatException e) {
      // Valor por defecto
      inputMeta.setCiviCrmPageSize(25);
    }

    try {
      String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
      String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
      String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());
      // String entity = wCiviCrmEntity.getText());

      RestUtil crUtil = new RestUtil(restUrl, apiKey, siteKey, wCiviCrmEntity.getText());

      HashMap<String, FieldAttrs> lFields = crUtil.getFieldLists(true);
      inputMeta.setFields(lFields);

      int nrKeys = tOutputFields.nrNonEmpty();

      if (nrKeys == 0) {
        throw new Exception(BaseMessages.getString(PKG, "CiviCrmStep.Exception.NoFieldsSelected"));
      }

      HashMap<String, String> hOutput = new HashMap<String, String>();

      for (int i = 0; i < nrKeys; i++) {
        TableItem item = tOutputFields.getNonEmpty(i);
        if (item.getText(1) != null && !item.getText(1).equals("")) {
          inputMeta.getKeyList().add(item.getText(1));
          hOutput.put(item.getText(1), (item.getText(2) != null && !item.getText(2).equals("")) ? item.getText(2) : item.getText(1));
        }
      }

      inputMeta.setOutputMap(hOutput);

      nrKeys = tFilterFields.nrNonEmpty();

      HashMap<String, String> hFilter = new HashMap<String, String>();
      for (int i = 0; i < nrKeys; i++) {
        TableItem item = tFilterFields.getNonEmpty(i);
        hFilter.put(item.getText(1), item.getText(2));
      }

      inputMeta.setFilterMap(hFilter);

      TransMeta previewMeta = TransPreviewFactory.generatePreviewTransformation(transMeta, inputMeta, wStepname.getText());

      EnterNumberDialog numberDialog = new EnterNumberDialog(shell, props.getDefaultPreviewSize(), BaseMessages.getString(PKG,
          "CiviCrmDialog.Preview.Title"), BaseMessages.getString(PKG, "CiviCrmDialog.Preview.NumberOfRowsToPreview")); //$NON-NLS-1$ //$NON-NLS-2$
      int previewSize = numberDialog.open();
      if (previewSize > 0) {
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
