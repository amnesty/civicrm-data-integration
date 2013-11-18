package es.stratebi.civi.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
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

import es.stratebi.civi.CiviDialog;
import es.stratebi.civi.CiviMeta;
import es.stratebi.civi.util.FieldAttrs;
import es.stratebi.civi.util.RestUtil;

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

  private Button wTestConnection;

  private Listener lsTestConnection;

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

    ModifyListener lsMod = new ModifyListener() {
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

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

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

    // Test connection button
    wTestConnection = new Button(gConnectionGroup, SWT.PUSH);
    wTestConnection.setText(BaseMessages.getString(PKG, "CiviCrmDialog.TestConnection.Button")); //$NON-NLS-1$

    // Get entity fields button testConnection
    wGetEntities = new Button(gConnectionGroup, SWT.PUSH);
    wGetEntities.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityList.Button")); //$NON-NLS-1$

    FormData fdGetEntities = new FormData();
    fdGetEntities.top = new FormAttachment(wCiviCrmApiKey, margin);
    fdGetEntities.right = new FormAttachment(100, 0);
    wGetEntities.setLayoutData(fdGetEntities);

    FormData fdTestConnection = new FormData();
    fdTestConnection.top = new FormAttachment(wCiviCrmApiKey, margin);
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

    // Get Entity Fields Button ----------------------------------
    Button wGetFields = new Button(gEntity, SWT.PUSH);
    wGetFields.setText(BaseMessages.getString(PKG, "CiviCrmDialog.GetEntityFields.Button"));
    FormData fdGetFields = new FormData();
    fdGetFields.top = new FormAttachment(gEntity, margin);
    fdGetFields.right = new FormAttachment(100, 0);
    wGetFields.setLayoutData(fdGetFields);

    FormData fdCiviCrmEntity = new FormData();
    fdCiviCrmEntity.top = new FormAttachment(gEntity, margin);
    fdCiviCrmEntity.left = new FormAttachment(middle, 0);
    fdCiviCrmEntity.right = new FormAttachment(wGetFields, 0);

    String[] contactEntity = { BaseMessages.getString(PKG, "CiviCrmDialog.objectType.Contacts") };

    wCiviCrmEntity.setLayoutData(fdCiviCrmEntity);
    wCiviCrmEntity.setItems(contactEntity);
    wCiviCrmEntity.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ((CiviOutputMeta) input).setChanged();

      }
    });

    // Add listeners
    Listener lsGetFields = new Listener() {
      public void handleEvent(Event e) {
        getEntityFields();
      }
    };

    wGetFields.addListener(SWT.Selection, lsGetFields);

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
    int outputKeyWidgetRows = (((CiviOutputMeta) input).getFields() != null ? ((CiviOutputMeta) input).getOutputMap().size() : 3);

    ColumnInfo[] ciFields = new ColumnInfo[outputKeyWidgetCols];
    streamFieldColumn = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldName"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        new String[] {}, false);
    outputFieldsColumn = new ColumnInfo(BaseMessages.getString(PKG, "CiviCrmDialog.ColumnInfo.FieldOutput"), ColumnInfo.COLUMN_TYPE_CCOMBO,
        new String[] {}, false);
    ciFields[0] = streamFieldColumn;
    ciFields[1] = outputFieldsColumn;

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

    /*************************************************
     * // OK AND CANCEL BUTTONS
     *************************************************/

    wOK = new Button(shell, SWT.PUSH);
    wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(gEntity, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOK, -margin);
    wTabFolder.setLayoutData(fdTabFolder);

    BaseStepDialog.positionBottomButtons(shell, new Button[] { wOK, wCancel }, margin, null);

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

    wCancel.addListener(SWT.Selection, lsCancel);
    wOK.addListener(SWT.Selection, lsOK);
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

  // Collect data from the meta and place it in the dialog
  public void getData() {
    super.getData();

    String comboValues[] = new String[0];
    if (((CiviOutputMeta) input).getFields() != null) {
      comboValues = new String[((CiviOutputMeta) input).getFields().size()];
      int i = 0;
      for (String cf : ((CiviOutputMeta) input).getFields().keySet()) {
        comboValues[i++] = cf;
      }
      Arrays.sort(comboValues);
    }

    /*
     * Si no hay campos de salida entonces automaticamente todos los campos que
     * vienen del paso anterior. Es responsabilidad del usuario si hace
     * modificaciones en los campos que llegan a este paso actualizar nuevamente
     * el listado de campo de salida
     */
    if ((((CiviOutputMeta) input).getOutputMap() != null) && (((CiviOutputMeta) input).getOutputMap().size() > 0)) {
      // Si hay elementos para filtrar entonces mostrarlos en la tabla
      int i = 0;
      String[] streamFields = new String[((CiviOutputMeta) input).getOutputMap().size()];
//      for (String cField : ((CiviOutputMeta) input).getOutputMap().keySet()) {
      for (String cField : ((CiviOutputMeta) input).getKeyList()) {
        TableItem item = tOutputFields.table.getItem(i);
        item.setText(1, cField);
        item.setText(2, ((CiviOutputMeta) input).getOutputMap().get(cField));
        streamFields[i++] = cField;
      }

      this.streamFieldColumn.setComboValues(streamFields);
    } else {
      // getInputFields(false);
    }

    this.outputFieldsColumn.setComboValues(comboValues);
    tOutputFields.optWidth(true);
    tOutputFields.removeEmptyRows();
    tOutputFields.setRowNums();
  }

  /*
   * Aqui se realiza una llamada al API de CIVICRM y se obtiene el listado de
   * campos que admite la entidad, los cuales son desplegados en las tablas que
   * muestran los campos y filtros
   */
  protected void getEntityFields() {
    try {
      if (((CiviOutputMeta) input).getFields().size() > 0) {
        MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
        boolean goOn = MessageDialog.openConfirm(shell, BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Title"),
            BaseMessages.getString(PKG, "CiviCrmDialog.DoMapping.ReplaceFields.Msg"));
        if (!goOn) {
          return;
        }
      }

      // Eliminamos los campos de salida y volvemos a actualizar la tabla con
      // los campos
      // de entrada mas los de la entidad seleccionada mapeando los que se
      // llamen igual
      tOutputFields.removeAll();

      if (prevFields != null) {
        prevFields.clear();
      }
      prevFields = transMeta.getPrevStepFields(stepname);
      if (prevFields != null && !prevFields.isEmpty()) {
        BaseStepDialog.getFieldsFromPrevious(prevFields, tOutputFields, 1, new int[] { 1, 2 }, new int[] {}, -1, -1, null);
        streamFieldColumn.setComboValues(prevFields.getFieldNames());

        /*
         * Verificamos que los elementos de salida siempre tengan un campo
         * valido de CIVICRM asignado si lo tiene, en caso de no tenerlo se
         * busca y se asigna si existe. Si lo tiene y no existe entonces se
         * reemplaza por el campo asociado si lo hubiera con igual nombre
         */
        HashMap<String, FieldAttrs> lFields = ((CiviOutputMeta) input).getFields();

        if (lFields != null && lFields.size() > 0) {
          int nrKeys = tOutputFields.nrNonEmpty();
          for (int i = 0; i < nrKeys; i++) {
            TableItem item = tOutputFields.getNonEmpty(i);
            String streamField = item.getText(1);
            if (streamField != null && !streamField.equals(""))
              if (lFields.get(streamField) != null) {
                item.setText(2, streamField);
              } else {
                item.setText(2, "");
              }
          }
        }
      }

      String restUrl = variables.environmentSubstitute(wCiviCrmRestUrl.getText());
      String apiKey = variables.environmentSubstitute(wCiviCrmApiKey.getText());
      String siteKey = variables.environmentSubstitute(wCiviCrmSiteKey.getText());

      RestUtil crUtil = new RestUtil(restUrl, apiKey, siteKey, "getfields", wCiviCrmEntity.getText());

      crUtil.setEntity(wCiviCrmEntity.getText());
      HashMap<String, FieldAttrs> lFields = crUtil.getFieldLists(true);
      ((CiviOutputMeta) input).setFields(lFields);

      String[] comboValues = new String[lFields.size()];

      int index = 0;
      // Actualizar campos de salida del plugin
      for (FieldAttrs field : lFields.values()) {
        comboValues[index++] = field.getfFieldKey();
      }

      // Verificamos que los elementos de salida siempre tengan un campo válido
      // de CIVICRM
      // asignado si lo tiene, en caso de no tenerlo se busca y se asigna si
      // existe. Si lo
      // tiene y no existe entonces se reemplaza por el campo asociado si lo
      // hubiera ocn
      // igual nombre
      int nrKeys = tOutputFields.nrNonEmpty();
      for (int i = 0; i < nrKeys; i++) {
        TableItem item = tOutputFields.getNonEmpty(i);
        String streamField = item.getText(1);
        // String outputField = item.getText(2);
        if (streamField != null && !streamField.equals(""))
          if (lFields.get(streamField) != null) {
            item.setText(2, streamField);
          } else {
            item.setText(2, "");
          }
      }

      Arrays.sort(comboValues);
      this.outputFieldsColumn.setComboValues(comboValues);

      tOutputFields.removeEmptyRows();
      tOutputFields.setRowNums();
      tOutputFields.optWidth(true);
    } catch (Exception e) {
      new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.EntityListError"), e.toString().split(":")[0], e); //$NON-NLS-1$ //$NON-NLS-2$
      logBasic(BaseMessages.getString(PKG, "CiviCrmStep.Error.APIExecError", e.toString()));
    }
  }
  
  protected boolean ok() {

    int nrKeys = tOutputFields.nrNonEmpty();
    boolean matchFields = true;

    HashMap<String, FieldAttrs> fields = ((CiviMeta) input).getFields();
    ArrayList<String> keyList = new ArrayList<String>();
    List<String> streamFields = Arrays.asList(streamFieldColumn.getComboValues());

    HashMap<String, String> hOutput = new HashMap<String, String>();

    for (int i = 0; i < nrKeys; i++) {
      TableItem item = tOutputFields.getNonEmpty(i);
      // Verificamos que los elementos de salida siempre tengan un campo
      // seleccionado y luego si no hay un alias le ponemos el mismo nombre del
      // campo
      
      if (item.getText(1) != null && !item.getText(1).equals("")) {
        String streamField = item.getText(1); 
        String fieldKey    = item.getText(2); 

        matchFields = (streamFields.indexOf(streamField) != -1);
        if (!matchFields) {
          String msg = "";
             msg = BaseMessages.getString(PKG, "CiviCrmStep.Error.StreamFieldNotMatch")
             .replace("$1", streamField);
          Exception e = new Exception();
          new ErrorDialog(shell, BaseMessages.getString(PKG, "CiviCrmStep.Error.UnableFindField"), msg, e);
          break;
        }

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

    if (matchFields) {
      stepname = wStepname.getText();

      ((CiviOutputMeta) input).setCiviCrmRestUrl(wCiviCrmRestUrl.getText());
      ((CiviOutputMeta) input).setCiviCrmApiKey(wCiviCrmApiKey.getText());
      ((CiviOutputMeta) input).setCiviCrmSiteKey(wCiviCrmSiteKey.getText());
      ((CiviOutputMeta) input).setCiviCrmEntity(wCiviCrmEntity.getText());

      ((CiviOutputMeta) input).setKeyList(keyList);
      ((CiviOutputMeta) input).setOutputMap(hOutput);
      dispose();
      return true;
    }
    
    return false;
  }


}
