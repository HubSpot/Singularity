import React, {Component} from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import MultiInput from '../common/formItems/MultiInput';
import DropDown from '../common/formItems/DropDown';
import CheckBox from '../common/formItems/CheckBox';
import RemoveButton from '../common/RemoveButton';
import Select from 'react-select';
import { modifyField, clearForm } from '../../actions/form';
import {SaveAction} from '../../actions/api/request';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';
import classNames from 'classnames';

const FORM_ID = 'newDeployForm';

const DEFAULT_EXECUTOR_TYPE = 'default';
const CUSTOM_EXECUTOR_TYPE = 'custom';

const FIELDS = {
  ALL: [
    'id',
    'executorType',
    {
      id: 'containerInfo',
      values: [
        'type',
        {
          id: 'docker',
          values: [
            'image',
            'network',
            'parameters',
            'privileged',
            'forcePullImage',
            'volumes',
            'portMappings'
          ]
        }
      ]
    }
  ],
  DEFAULT_EXECUTOR: [
    'command',
    'uris',
    'arguments'
  ],
  CUSTOM_EXECUTOR: [
    'customExecutorCmd',
    {
      id: 'executorData',
      values: [
        'cmd',
        'extraCmdLineArgs',
        'user',
        'sigKillProcessesAfterMillis',
        'successfulExitCodes',
        'maxTaskThreads',
        'loggingTag',
        'loggingExtraFields',
        'preserveTaskSandboxAfterFinish',
        'skipLogrotateAndCompress',
        'embeddedArtifacts',
        'externalArtifacts',
        's3Artifacts'
      ]
    }
  ]
};

class NewDeployForm extends Component {

  componentDidMount() {
    this.props.clearForm(FORM_ID);
  }

  updateField(fieldId, newValue) {
    this.props.update(FORM_ID, fieldId, newValue);
  }

  getValue(fieldId) {
    if (this.props.form && this.props.form[fieldId]) {
      return this.props.form[fieldId];
    } else {
      return undefined;
    }
  }

  cantSubmit() {
    return true;
  }

  getExecutorType() {
    return this.getValue('executorType') || DEFAULT_EXECUTOR_TYPE;
  }

  getContainerType() {
    return this.getValue('containerType') || 'mesos';
  }

  addThingToArrayField(fieldId, thing) {
    if (!this.getValue(fieldId)) {
      this.updateField(fieldId, [thing]);
    } else {
      const fieldValue = this.getValue(fieldId).slice();
      fieldValue.push(thing);
      this.updateField(fieldId, fieldValue);
    }
  }

  addThingPreventDefault(fieldId, thing, event) {
    event.preventDefault();
    this.addThingToArrayField(fieldId, thing);
  }

  removeThingFromArrayField(fieldId, key) {
    const fieldValue = this.getValue(fieldId).slice();
    fieldValue.splice(key, 1);
    this.updateField(fieldId, fieldValue);
  }

  updateThingInArrayField(fieldId, key, newFieldObj) {
    const newArray = this.getValue(fieldId).slice();
    const newValue = _.extend({}, newArray[key], newFieldObj);
    newArray[key] = newValue;
    this.updateField(fieldId, newArray);
  }

  renderDefaultExecutorFields() {
    const cmdLineArguments = (
      <div className="form-group">
        <label htmlFor="cmd-line-args">Arguments</label>
        <MultiInput
          id = "cmd-line-args"
          value = {this.getValue('arguments') || []}
          onChange = {(newValue) => this.updateField('arguments', newValue)}
        />
      </div>
    );
    const artifacts = (
      <div className="form-group">
        <label htmlFor="artifacts" >Artifacts</label>
        <MultiInput
          id = "artifacts"
          value = {this.getValue('uris') || []}
          onChange = {(newValue) => this.updateField('uris', newValue)}
          placeholder="eg: http://s3.example/my-artifact"
        />
      </div>
    );
    return (
      <div>
        <fieldset id="default-expandable" className='expandable'>
          <h4>Default Executor Settings</h4>
          {cmdLineArguments}
          {artifacts}
        </fieldset>
      </div>
    );
  }

  renderArtifact(artifact, key) {
    const name = (
      <div className="form-group required">
        <label htmlFor={`name-${ key }`}>Name</label>
        <FormField
          id={`name-${ key }`}
          className='name'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {name: event.target.value}),
            inputType: 'text',
            value: artifact["name"],
            required: true
          }}
        />
      </div>
    );
    const fileName = (
      <div className="form-group required">
        <label htmlFor={`filename-${key}`}>File name</label>
        <FormField
          id={`filename-${ key }`}
          className='filename'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {filename: event.target.value}),
            inputType: 'text',
            value: artifact["filename"],
            required: true
          }}
        />
      </div>
    );
    const md5Sum = (
      <div className="form-group">
        <label htmlFor={`md5-${ key }`}>MD5 checksum</label>
        <FormField
          id={`md5-${ key }`}
          className='md5'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {md5Sum: event.target.value}),
            inputType: 'text',
            value: artifact["md5Sum"]
          }}
        />
      </div>
    );
    const content = (
      <div className="form-group">
        <label htmlFor={`content-${ key }`}>Content</label>
        <FormField
          id={`content-${ key }`}
          className='content'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {content: event.target.value}),
            inputType: 'text',
            value: artifact["content"]
          }}
        />
      </div>
    );
    const filesize = (
      <div className="form-group">
        <label htmlFor={`file-size-${ key }`}>File size</label>
        <FormField
          id={`file-size-${ key }`}
          className='filesize'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {filesize: event.target.value}),
            inputType: 'text',
            value: artifact["filesize"]
          }}
        />
      </div>
    );
    const url = (
      <div className="form-group required">
        <label htmlFor={`url-${ key }`}>URL</label>
        <FormField
          id={`url-${ key }`}
          className='url'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {url: event.target.value}),
            inputType: 'text',
            value: artifact["url"],
            required: true
          }}
        />
      </div>
    );
    const s3Bucket = (
      <div className="form-group required">
        <label htmlFor={`bucket-${ key }`}>S3 bucket</label>
        <FormField
          id={`bucket-${ key }`}
          className='bucket'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {s3Bucket: event.target.value}),
            inputType: 'text',
            value: artifact["s3Bucket"],
            required: true
          }}
        />
      </div>
    );
    const s3ObjectKey = (
      <div className="form-group required">
        <label htmlFor={`object-key-${ key }`}>S3 object key</label>
        <FormField
          id={`object-key-${ key }`}
          className='object-key'
          prop = {{
            updateFn: event => this.updateThingInArrayField("artifacts", key, {s3ObjectKey: event.target.value}),
            inputType: 'text',
            value: artifact["s3ObjectKey"],
            required: true
          }}
        />
      </div>
    );
    return (
      <div key={key} className="well well-sm artifact">
        <h5>{artifact.type} artifact</h5>
        <RemoveButton
          id={`remove-artifact-${key}`}
          onClick={() => { this.removeThingFromArrayField('artifacts', key) }} />
        {name}
        {fileName}
        {md5Sum}
        {artifact.type === 'embedded' && content}
        {artifact.type !== 'embedded' && filesize}
        {artifact.type === 'external' && url}
        {artifact.type === 's3' && s3Bucket}
        {artifact.type === 's3' && s3ObjectKey}
      </div>
    );
  }

  renderCustomArtifactFields() {
    if (!this.getValue('artifacts')) {
      return (<div id="custom-artifacts"></div>);
    }
    return this.getValue('artifacts').map((artifact, key) => { return this.renderArtifact(artifact, key) });
  }

  renderCustomExecutorFields() {
    const customExecutorCmds = (
      <div className="form-group required">
        <label htmlFor="custom-executor-command">Custom executor command</label>
        <FormField
          id = "customExecutorCmd"
          prop = {{
            updateFn: event => this.updateField("customExecutorCmd", event.target.value),
            inputType: 'text',
            value: this.getValue("customExecutorCmd"),
            required: true,
            placeholder: "eg: /usr/local/bin/singularity-executor"
          }}
        />
      </div>
    );
    const extraCommandArgs = (
      <div className="form-group">
        <label htmlFor="extra-args">Extra command args</label>
        <MultiInput
          id = "extra-args"
          value = {this.getValue('extraCmdLineArgs') || []}
          onChange = {(newValue) => this.updateField('extraCmdLineArgs', newValue)}
          placeholder="eg: -jar MyThing.jar"
        />
      </div>
    );
    const user = (
      <div className="form-group">
        <label htmlFor="user">User</label>
        <FormField
          id = "user"
          prop = {{
            updateFn: event => this.updateField("user", event.target.value),
            inputType: 'text',
            value: this.getValue("user"),
            placeholder: "default: root"
          }}
        />
      </div>
    );
    const killAfterMillis = (
      <div className="form-group">
        <label htmlFor="kill-after-millis">Kill processes after (milisec)</label>
        <FormField
          id = "kill-after-millis"
          prop = {{
            updateFn: event => this.updateField("sigKillProcessesAfterMillis", event.target.value),
            inputType: 'text',
            value: this.getValue("sigKillProcessesAfterMillis"),
            placeholder: "default: 120000"
          }}
        />
      </div>
    );
    const successfulExitCodes = (
      <div className="form-group">
        <label htmlFor="successful-exit-code">Successful exit codes</label>
        <MultiInput
          id = "successful-exit-code"
          value = {this.getValue("successfulExitCodes") || []}
          onChange = {(newValue) => this.updateField("successfulExitCodes", newValue)}
        />
      </div>
    );
    const maxTaskThreads = (
      <div className="form-group">
        <label htmlFor="max-task-threads">Max Task Threads</label>
        <FormField
          id = "max-task-threads"
          prop = {{
            updateFn: event => this.updateField("maxTaskThreads", event.target.value),
            inputType: 'text',
            value: this.getValue("maxTaskThreads")
          }}
        />
      </div>
    );
    const loggingTag = (
      <div className="form-group">
        <label htmlFor="logging-tag">Logging tag</label>
        <FormField
          id = "logging-tag"
          prop = {{
            updateFn: event => this.updateField("loggingTag", event.target.value),
            inputType: 'text',
            value: this.getValue("loggingTag")
          }}
        />
      </div>
    );
    const loggingExtraFields = (
      <div className="form-group">
        <label htmlFor="logging-extra-fields">Logging extra fields</label>
        <MultiInput
          id = "logging-extra-fields"
          value = {this.getValue("loggingExtraFields") || []}
          onChange = {(newValue) => this.updateField("loggingExtraFields", newValue)}
          placeholder="format: key=value"
        />
      </div>
    );
    const preserveSandbox = (
      <div className="form-group">
        <label htmlFor="preserve-sandbox">
          <CheckBox
            id = "preserve-sandbox"
            onChange = {event => this.updateField("preserveTaskSandboxAfterFinish", !this.getValue("preserveTaskSandboxAfterFinish"))}
            checked = {this.getValue("preserveTaskSandboxAfterFinish")}
            noFormControlClass = {true}
          />
          {" Preserve task sandbox after finish"}
        </label>
      </div>
    );
    const skipLogrotateAndCompress = (
      <div className="form-group">
        <label htmlFor="skip-lr-compress">
          <CheckBox
            id = "skip-lr-compress"
            onChange = {event => this.updateField("skipLogrotateAndCompress", !this.getValue("skipLogrotateAndCompress"))}
            checked = {this.getValue("skipLogrotateAndCompress")}
            noFormControlClass = {true}
          />
          {" Skip lorotate compress"}
        </label>
      </div>
    );
    const loggingS3Bucket = (
      <div className="form-group">
        <label htmlFor="logging-s3-bucket">Logging S3 Bucket</label>
        <FormField
          id = "logging-s3-bucket"
          prop = {{
            updateFn: event => this.updateField("loggingS3Bucket", event.target.value),
            inputType: 'text',
            value: this.getValue("loggingS3Bucket")
          }}
        />
      </div>
    );
    const maxOpenFiles = (
      <div className="form-group">
        <label htmlFor="max-open-files">Max Open Files</label>
        <FormField
          id = "max-open-files"
          prop = {{
            updateFn: event => this.updateField("maxOpenFiles", event.target.value),
            inputType: 'text',
            value: this.getValue("maxOpenFiles")
          }}
        />
      </div>
    );
    const runningSentinel = (
      <div className="form-group">
        <label htmlFor="running-sentinel">Running Sentinel</label>
        <FormField
          id = "running-sentinel"
          prop = {{
            updateFn: event => this.updateField("runningSentinel", event.target.value),
            inputType: 'text',
            value: this.getValue("runningSentinel")
          }}
        />
      </div>
    );
    return (
      <div>
        <fieldset>
          <h4>Custom Executor Settingss</h4>

          {customExecutorCmds}
          {extraCommandArgs}

          <div className="row">
            <div className="col-md-6">
              {user}
            </div>
            <div className="col-md-6">
              {killAfterMillis}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {successfulExitCodes}
            </div>
            <div className="col-md-6">
              {maxTaskThreads}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {loggingTag}
            </div>
            <div className="col-md-6">
              {loggingExtraFields}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {preserveSandbox}
            </div>
            <div className="col-md-6">
              {skipLogrotateAndCompress}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {loggingS3Bucket}
            </div>
            <div className="col-md-6">
              {maxOpenFiles}
            </div>
          </div>

          {runningSentinel}
        </fieldset>

        <fieldset>
          <h4>Custom executor artifacts</h4>

          { this.renderCustomArtifactFields() }

          <div id="artifact-button-row" className="row">
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addThingPreventDefault('artifacts', {type: 'embedded'}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Embedded"}
              </button>
            </div>
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addThingPreventDefault('artifacts', {type: 'external'}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" External"}
              </button>
            </div>
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addThingPreventDefault('artifacts', {type: 's3'}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" S3"}
              </button>
            </div>
          </div>
        </fieldset>
      </div>
    );
  }

  renderDockerPortMapping(mapping, key) {
    const thisPortMapping = this.getValue('portMappings')[key];
    const containerPortType = (
      <div className="form-group required">
        <label htmlFor={`cont-port-type-${ key }`}>Container Port Type</label>
        <Select
          id={`cont-port-type-${ key }`}
          className="cont-port-type"
          options={[
            { label: 'Literal', value: 'LITERAL' },
            { label: 'From Offer', value: 'FROM_OFFER' }
          ]}
          onChange={newValue => this.updateThingInArrayField('portMappings', key, {containerPortType: newValue.value})}
          value={thisPortMapping.containerPortType || 'LITERAL'}
          clearable={false}
        />
      </div>
    );
    const containerPort = (
      <div className="form-group required">
        <label htmlFor={`cont-port-${ key }`}>Container Port</label>
        <FormField
          id={`cont-port-${ key }`}
          className="cont-port"
          prop = {{
            updateFn: event => this.updateThingInArrayField('portMappings', key, {containerPort: event.target.value}),
            inputType: 'text',
            value: thisPortMapping.containerPort,
            required: true
          }}
        />
      </div>
    );
    const hostPortType = (
      <div className="form-group required">
        <label htmlFor={`host-port-type-${ key }`}>Host Port Type</label>
        <Select
          id={`host-port-type-${ key }`}
          className="host-port-type"
          options={[
            { label: 'Literal', value: 'LITERAL' },
            { label: 'From Offer', value: 'FROM_OFFER' }
          ]}
          onChange={newValue => this.updateThingInArrayField('portMappings', key, {hostPortType: newValue.value})}
          value={thisPortMapping.hostPortType || 'LITERAL'}
          clearable={false}
        />
      </div>
    );
    const hostPort = (
      <div className="form-group required">
        <label htmlFor={`host-port-${ key }`}>Host Port</label>
        <FormField
          id={`host-port-${ key }`}
          className="host-port"
          prop = {{
            updateFn: event => this.updateThingInArrayField('portMappings', key, {hostPort: event.target.value}),
            inputType: 'text',
            value: thisPortMapping.hostPort,
            required: true
          }}
        />
      </div>
    );
    const protocol = (
      <div className="form-group">
        <label htmlFor={`protocol-${ key }`}>Protocol</label>
        <FormField
          id={`protocol-${ key }`}
          className="protocol"
          prop = {{
            updateFn: event => this.updateThingInArrayField('portMappings', key, {protocol: event.target.value}),
            inputType: 'text',
            value: thisPortMapping.protocol,
            placeholder: "default: tcp"
          }}
        />
      </div>
    );
    return (
      <div className="well well-sm docker-port" key={key}>
        <h5>Docker Port Mapping</h5>
        <RemoveButton
          id={`remove-port-mapping-${key}`}
          onClick={() => { this.removeThingFromArrayField('portMappings', key) }} />
        {containerPortType}
        {containerPort}
        {hostPortType}
        {hostPort}
        {protocol}
      </div>
    );
  }

  renderDockerPortMappings() {
    const portMappings = this.getValue('portMappings');
    if (!portMappings) {
      return (<div id="docker-port-mappings" />);
    }
    return portMappings.map((mapping, key) => this.renderDockerPortMapping(mapping, key));
  }

  renderDockerVolume(mapping, key) {
    const thisVolume = this.getValue('volumes')[key];
    const containerPath = (
      <div className="form-group required">
        <label htmlFor={`cont-path-${ key }`}>Container Path</label>
        <FormField
          id={`cont-path-${ key }`}
          className="cont-path"
          prop = {{
            updateFn: event => this.updateThingInArrayField('volumes', key, {containerPath: event.target.value}),
            inputType: 'text',
            value: thisVolume.containerPath,
            required: true
          }}
        />
      </div>
    );
    const hostPath = (
      <div className="form-group required">
        <label htmlFor={`host-path-${ key }`}>Host Path</label>
        <FormField
          id={`host-path-${ key }`}
          className="host-path"
          prop = {{
            updateFn: event => this.updateThingInArrayField('volumes', key, {hostPath: event.target.value}),
            inputType: 'text',
            value: thisVolume.hostPath,
            required: true
          }}
        />
      </div>
    );
    const mode = (
      <div className="form-group required">
        <label htmlFor={`volume-mode-${ key }`}>Volume Mode</label>
        <Select
          id={`volume-mode-${ key }`}
          className="volume-mode"
          options={[
            { label: 'RO', value: 'RO' },
            { label: 'RW', value: 'RW' }
          ]}
          onChange={newValue => this.updateThingInArrayField('volumes', key, {mode: newValue.value})}
          value={thisVolume.mode || 'RO'}
          clearable={false}
        />
      </div>
    );
    return (
      <div className="well well-sm docker-volume" key={key}>
        <h5>Docker Volume</h5>
        <RemoveButton
          id={`remove-volume-${key}`}
          onClick={() => { this.removeThingFromArrayField('volumes', key) }} />
        {containerPath}
        {hostPath}
        {mode}
      </div>
    );
  }

  renderDockerVolumes() {
    const volumes = this.getValue('volumes');
    if (!volumes) {
      return (<div id="docker-volumes" />);
    }
    return volumes.map((mapping, key) => this.renderDockerVolume(mapping, key));
  }

  renderDockerContainerFields() {
    const image = (
      <div className="form-group required">
        <label htmlFor="docker">Docker image</label>
        <FormField
          id = "docker"
          prop = {{
            updateFn: event => this.updateField("image", event.target.value),
            inputType: 'text',
            value: this.getValue("image"),
            required: true,
            placeholder: "eg: centos6:latest"
          }}
        />
      </div>
    );
    const network = (
      <div className="form-group">
        <label htmlFor="dockernetwork">Docker Network</label>
        <Select
          id="dockernetwork"
          options={[
            { label: 'None', value: 'NONE' },
            { label: 'Bridge', value: 'BRIDGE' },
            { label: 'Host', value: 'HOST' }
          ]}
          onChange={newValue => this.updateField('network', newValue.value)}
          value={this.getValue('network') || 'NONE'}
          clearable={false}
        />
      </div>
    );
    const privileged = (
      <div className="form-group">
        <label htmlFor="privileged">
          <CheckBox
            id = "privileged"
            onChange = {event => this.updateField("privileged", !this.getValue("privileged"))}
            checked = {this.getValue("privileged")}
            noFormControlClass = {true}
          />
          {" Privileged"}
        </label>
      </div>
    );
    const forcePullImage = (
      <div className="form-group">
        <label htmlFor="force-pull">
          <CheckBox
            id = "force-pull"
            onChange = {event => this.updateField("forcePullImage", !this.getValue("forcePullImage"))}
            checked = {this.getValue("forcePullImage")}
            noFormControlClass = {true}
          />
          {" Force Pull Image"}
        </label>
      </div>
    );
    const parameters = (
      <div className="form-group">
        <label for="docker-params">Docker Parameters</label>
        <MultiInput
          id = "docker-params"
          value = {this.getValue("parameters") || []}
          onChange = {(newValue) => this.updateField("parameters", newValue)}
          placeholder="format: key=value"
        />
      </div>
    )
    //
    return (
      <div className="container-info">
        <fieldset>
          <h4>Docker Settings</h4>

          {image}
          {network}

          <div className="row">
            <div className="col-md-6">
              {privileged}
            </div>
            <div className="col-md-6">
              {forcePullImage}
            </div>
          </div>

          {parameters}

          {this.renderDockerPortMappings()}

          <div id="docker-port-button-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addThingPreventDefault('portMappings', {}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Docker Port Mapping"}
              </button>
            </div>
          </div>

          {this.renderDockerVolumes()}

          <div id="docker-volume-button-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addThingPreventDefault('volumes', {}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Docker Volume"}
              </button>
            </div>
          </div>

        </fieldset>
      </div>
    );
  }

  render() {
    // Fields
    const deployId = (
      <div className="form-group required">
        <label htmlFor="id">Deploy ID</label>
        <FormField
          id = "id"
          prop = {{
            updateFn: event => this.updateField("id", event.target.value),
            inputType: 'text',
            value: this.getValue("id"),
            required: true
          }}
        />
      </div>
    );
    const executorType = (
      <div className="form-group required">
        <label htmlFor="executor-type">Executor type</label>
        <Select
          id = 'executor-type'
          options={[
            { label: 'Default', value: DEFAULT_EXECUTOR_TYPE },
            { label: 'Custom', value: CUSTOM_EXECUTOR_TYPE }
          ]}
          onChange={newValue => this.updateField('executorType', newValue.value)}
          value={this.getExecutorType()}
          clearable={false}
        />
      </div>
    );
    const command = (
      <div className="form-group">
        <label htmlFor="command">Command to execute</label>
        <FormField
          id = "command"
          prop = {{
            updateFn: event => this.updateField("command", event.target.value),
            inputType: 'text',
            value: this.getValue("command"),
            placeholder: "eg: rm -rf /"
          }}
        />
      </div>
    );
    const containerType = (
      <div className="form-group required">
        <label htmlFor="container-type">Container type</label>
        <Select
          id="container-type"
          options={[
            { label: 'Mesos', value: 'mesos' },
            { label: 'Docker', value: 'docker' }
          ]}
          onChange={newValue => this.updateField('containerType', newValue.value)}
          required={true}
          value={this.getValue('containerType') || 'mesos'}
          clearable={false}
        />
      </div>
    );

    // Info Groups
    const executorInfo = (
      <div className="well">
        <div className="row">
          <div className="col-md-4">
              <h3>Executor Info</h3>
          </div>
          <div className="col-md-8">
              {executorType}
          </div>
        </div>
        {command}
        { this.getExecutorType() === DEFAULT_EXECUTOR_TYPE && this.renderDefaultExecutorFields() }
        { this.getExecutorType() === CUSTOM_EXECUTOR_TYPE && this.renderCustomExecutorFields() }
      </div>
    );
    const containerInfo = (
      <div className="well">
        <div className="row">
          <div className="col-md-4">
            <h3>Container Info</h3>
          </div>
          <div className="col-md-8">
            {containerType}
          </div>
        </div>

        { this.getContainerType() === 'docker' && this.renderDockerContainerFields() }
      </div>
    );
    return (
      <div>
        <h2>
          New deploy for <a href={`${ config.appRoot }/request/${ this.props.request.id }`}>{ this.props.request.id }</a>
        </h2>
        <div className="row new-form">
          <form className="col-md-8">

            {deployId}
            {executorInfo}
            {containerInfo}

          </form>
          <div id="help-column" class="col-md-4 col-md-offset-1" />
        </div>
      </div>
    );
  }

}

function mapStateToProps(state) {
  return {
    request: state.api.request.data.request,
    form: state.form[FORM_ID],
    saveApiCall: state.api.saveDeploy
  }
}

function mapDispatchToProps(dispatch) {
  return {
    update(formId, fieldId, newValue) {
      dispatch(modifyField(formId, fieldId, newValue));
    },
    clearForm(formId) {
      dispatch(clearForm(formId));
    },
    save(requestBody) {
      dispatch(SaveAction.trigger(requestBody));
    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(NewDeployForm);
