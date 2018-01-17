import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link, withRouter } from 'react-router';

import Utils from '../../utils';

import SelectFormGroup from '../common/formItems/formGroups/SelectFormGroup';
import TextFormGroup from '../common/formItems/formGroups/TextFormGroup';
import MultiInputFormGroup from '../common/formItems/formGroups/MultiInputFormGroup';
import CheckboxFormGroup from '../common/formItems/formGroups/CheckboxFormGroup';

import { ModifyField, ClearForm } from '../../actions/ui/form';
import { SaveDeploy } from '../../actions/api/deploys';
import { FetchRequest } from '../../actions/api/requests';
import { refresh } from '../../actions/ui/newDeployForm';

import {
  FIELDS, ARTIFACT_FIELDS, DOCKER_PORT_MAPPING_FIELDS, DOCKER_VOLUME_FIELDS,
  INDEXED_FIELDS, INDEXED_ARTIFACT_FIELDS, INDEXED_DOCKER_PORT_MAPPING_FIELDS,
  INDEXED_DOCKER_VOLUME_FIELDS, INDEXED_ALL_FIELDS,
  INDEXED_MESOS_ARTIFACT_FIELDS, MESOS_ARTIFACT_FIELDS,
  INDEXED_CUSTOM_EXECUTOR_FIELDS, INDEXED_DEFAULT_EXECUTOR_FIELDS,
  INDEXED_DOCKER_CONTAINER_FIELDS, INDEXED_LOAD_BALANCER_FIELDS,
  INDEXED_HEALTH_CHECKER_FIELDS, INDEXED_ALL_ARTIFACT_FIELDS,
  INDEXED_EMBEDDED_ARTIFACT_FIELDS, INDEXED_EXTERNAL_ARTIFACT_FIELDS,
  INDEXED_S3_ARTIFACT_FIELDS, INDEXED_DOCKER_PARAMETERS_FIELDS, DOCKER_PARAMETERS_FIELDS
} from './fields';


const FORM_ID = 'newDeployForm';

const DEFAULT_EXECUTOR_TYPE = 'default';
const CUSTOM_EXECUTOR_TYPE = 'custom';
const ARTIFACT_SHAPE = PropTypes.shape({
  name: PropTypes.string,
  type: PropTypes.oneOf(['embedded', 'external', 's3']).isRequired,
  filename: PropTypes.string,
  md5Sum: PropTypes.string,
  content: PropTypes.string,
  url: PropTypes.string,
  filesize: PropTypes.string,
  s3Bucket: PropTypes.string,
  s3ObjectKey: PropTypes.string
});

class NewDeployForm extends Component {

  static propTypes = {
    form: PropTypes.shape({
      arguments: PropTypes.arrayOf(PropTypes.string),
      uris: PropTypes.arrayOf(PropTypes.shape({
        uri: PropTypes.string,
        cache: PropTypes.bool,
        executable: PropTypes.bool,
        extract: PropTypes.bool
      })),
      embeddedArtifacts: PropTypes.arrayOf(ARTIFACT_SHAPE),
      externalArtifacts: PropTypes.arrayOf(ARTIFACT_SHAPE),
      s3Artifacts: PropTypes.arrayOf(ARTIFACT_SHAPE),
      cmd: PropTypes.string,
      extraCmdLineArgs: PropTypes.arrayOf(PropTypes.string),
      user: PropTypes.string,
      sigKillProcessesAfterMillis: PropTypes.string,
      successfulExitCodes: PropTypes.arrayOf(PropTypes.string),
      maxTaskThreads: PropTypes.string,
      loggingTag: PropTypes.string,
      loggingExtraFields: PropTypes.arrayOf(PropTypes.string),
      logrotateFrequency: PropTypes.string,
      preserveTaskSandboxAfterFinish: PropTypes.bool,
      skipLogrotateAndCompress: PropTypes.bool,
      loggingS3Bucket: PropTypes.string,
      maxOpenFiles: PropTypes.string,
      runningSentinel: PropTypes.string,
      portMappings: PropTypes.arrayOf(PropTypes.shape({
        containerPortType: PropTypes.string,
        containerPort: PropTypes.string,
        hostPortType: PropTypes.string,
        hostPort: PropTypes.string,
        protocol: PropTypes.string
      })),
      dockerParameters: PropTypes.arrayOf(PropTypes.shape({
        key: PropTypes.string,
        value: PropTypes.string
      })),
      volumes: PropTypes.arrayOf(PropTypes.shape({
        containerPath: PropTypes.string,
        hostPath: PropTypes.string,
        mode: PropTypes.string
      })),
      image: PropTypes.string,
      privileged: PropTypes.bool,
      forcePullImage: PropTypes.bool,
      parameters: PropTypes.arrayOf(PropTypes.string),
      id: PropTypes.string,
      command: PropTypes.string,
      type: PropTypes.string,
      cpus: PropTypes.string,
      memoryMb: PropTypes.string,
      numPorts: PropTypes.string,
      diskMb: PropTypes.string,
      env: PropTypes.arrayOf(PropTypes.string),
      healthcheckUri: PropTypes.string,
      healthcheckPortIndex: PropTypes.string,
      healthcheckPortNumber: PropTypes.string,
      healthcheckStartupDelaySeconds: PropTypes.string,
      healthcheckStartupTimeoutSeconds: PropTypes.string,
      healthcheckStartupIntervalSeconds: PropTypes.string,
      healthcheckTimeoutSeconds: PropTypes.string,
      healthcheckIntervalSeconds: PropTypes.string,
      healthcheckMaxRetries: PropTypes.string,
      failureStatusCodes: PropTypes.arrayOf(PropTypes.string),
      deployHealthTimeoutSeconds: PropTypes.string,
      skipHealthchecksOnDeploy: PropTypes.bool,
      considerHealthyAfterRunningForSeconds: PropTypes.string,
      serviceBasePath: PropTypes.string,
      loadBalancerGroups: PropTypes.arrayOf(PropTypes.string),
      loadBalancerOptions: PropTypes.arrayOf(PropTypes.string),
      loadBalancerPortIndex: PropTypes.string,
      unpauseOnSuccessfulDeploy: PropTypes.bool
    }).isRequired,
    request: PropTypes.shape({
      state: PropTypes.string.isRequired,
      request: PropTypes.shape({
        requestType: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired,
        loadBalanced: PropTypes.bool
      }).isRequired
    }).isRequired,
    saveApiCall: PropTypes.shape({
      error: PropTypes.string,
      data: PropTypes.shape({
        message: PropTypes.string,
        activeDeploy: PropTypes.shape({
          id: PropTypes.string,
          requestId: PropTypes.string
        }),
        pendingDeploy: PropTypes.shape({
          id: PropTypes.string,
          requestId: PropTypes.string
        })
      })
    }),
    clearForm: PropTypes.func.isRequired,
    clearSaveDeployData: PropTypes.func.isRequired,
    update: PropTypes.func.isRequired,
    save: PropTypes.func.isRequired
  };

  componentDidMount() {
    this.props.clearForm();
    this.props.clearSaveDeployData();
  }

  updateField(fieldId, newValue) {
    this.props.update(FORM_ID, fieldId, newValue);
  }

  getValueOrDefault(fieldId) {
    return this.props.form[fieldId] || INDEXED_FIELDS[fieldId].default;
  }

  isRequestDaemon() {
    return ['SERVICE', 'WORKER'].indexOf(this.props.request.request.requestType) !== -1;
  }

  // Returns true unless the object is falsey or an empty array.
  hasValue(value) {
    if (!value) {
      return false;
    }
    if (Array.isArray(value) && _.isEmpty(value)) {
      return false;
    }
    return true;
  }

  validateValue(value, type, arrayType) {
    if (!value) {
      return true;
    }
    if (type === 'number') {
      const number = parseFloat(value, 10);
      return number === 0 || number; // NaN is invalid
    } else if (type === 'map') {
      for (const element of value) {
        if (element.split('=').length !== 2) {
          return false;
        }
      }
    } else if (type === 'array') {
      for (const element of value) {
        if (!this.validateValue(element, arrayType)) {
          return false;
        }
      }
    } else if (type === 'mapPair') {
      if (value.split('=').length !== 2) {
        return false;
      }
    }
    return true;
  }

  errorsInArrayField(field, valueGetter) {
    const errorIndices = [];
    const arrayFieldValue = valueGetter(field.id);
    if (field.required && _.isEmpty(arrayFieldValue)) {
      return [0];
    }
    const type = field.type === 'map' && 'mapPair' || field.arrayType;
    for (const idx in arrayFieldValue) {
      if (!this.validateValue(arrayFieldValue[idx], type)) {
        errorIndices.push(parseInt(idx, 10));
      }
    }
    return errorIndices;
  }

  validateField(field, valueGetter) {
    const type = field.type;
    if (type === 'object') {
      for (const subField of field.values) {
        if (!this.validateField(subField)) {
          return false;
        }
      }
      return true;
    }
    const value = valueGetter(field.id);
    if (field.required && !this.hasValue(value)) {
      return false;
    }
    return this.validateValue(value, type, field.arrayType);
  }

  formFieldFeedback(field, value) {
    if (!field.required && !value) {
      return null;
    }
    if (field.required && !value) {
      return 'ERROR';
    }
    if (this.validateField(field, () => value)) {
      return 'SUCCESS';
    }
    return 'ERROR';
  }

  validateFields(fields) {
    for (const fieldId of Object.keys(fields)) {
      if (!this.validateField(fields[fieldId], (localFieldId) => this.getValueOrDefault(localFieldId))) {
        return false;
      }
    }
    return true;
  }

  validateObject(obj, fieldsToValidateAgainst) {
    for (const fieldId of Object.keys(fieldsToValidateAgainst)) {
      if (!this.validateField(fieldsToValidateAgainst[fieldId], (localFieldId) => obj[localFieldId] || fieldsToValidateAgainst[localFieldId].default)) {
        return false;
      }
    }
    return true;
  }

  validateObjects(idForObjects, fieldsToValidateAgainst) {
    const objects = this.getValueOrDefault(idForObjects);
    if (!objects) {
      return true;
    }
    for (const id of Object.keys(objects)) {
      if (!this.validateObject(objects[id], fieldsToValidateAgainst)) {
        return false;
      }
    }
    return true;
  }

  validateArtifacts() {
    for (const artifact of this.getValueOrDefault('embeddedArtifacts') || []) {
      if (!this.validateObject(artifact, INDEXED_ALL_ARTIFACT_FIELDS)) {
        return false;
      }
      if (!this.validateObject(artifact, INDEXED_EMBEDDED_ARTIFACT_FIELDS)) {
        return false;
      }
    }
    for (const artifact of this.getValueOrDefault('externalArtifacts') || []) {
      if (!this.validateObject(artifact, INDEXED_ALL_ARTIFACT_FIELDS)) {
        return false;
      }
      if (!this.validateObject(artifact, INDEXED_EXTERNAL_ARTIFACT_FIELDS)) {
        return false;
      }
    }
    for (const artifact of this.getValueOrDefault('s3Artifacts') || []) {
      if (!this.validateObject(artifact, INDEXED_ALL_ARTIFACT_FIELDS)) {
        return false;
      }
      if (artifact.type === 's3' && !this.validateObject(artifact, INDEXED_S3_ARTIFACT_FIELDS)) {
        return false;
      }
    }
    return true;
  }

  canSubmit() {
    if (!this.validateFields(INDEXED_ALL_FIELDS)) {
      return false;
    }
    if (this.getValueOrDefault('executorType') === CUSTOM_EXECUTOR_TYPE) {
      if (!this.validateFields(INDEXED_CUSTOM_EXECUTOR_FIELDS) || !this.validateArtifacts()) {
        return false;
      }
    } else if (!this.validateFields(INDEXED_DEFAULT_EXECUTOR_FIELDS)) {
      return false;
    }
    if (this.getValueOrDefault('type') === 'DOCKER') {
      if (!this.validateFields(INDEXED_DOCKER_CONTAINER_FIELDS) ||
        !this.validateObjects('portMappings', INDEXED_DOCKER_PORT_MAPPING_FIELDS) ||
        !this.validateObjects('volumes', INDEXED_DOCKER_VOLUME_FIELDS) ||
        !this.validateObjects('dockerParameters', INDEXED_DOCKER_PARAMETERS_FIELDS)) {
        return false;
      }
    }
    if (this.props.request.request.loadBalanced && !this.validateFields(INDEXED_LOAD_BALANCER_FIELDS)) {
      return false;
    }
    if (this.isRequestDaemon() && !this.validateFields(INDEXED_HEALTH_CHECKER_FIELDS)) {
      return false;
    }
    return true;
  }

  copyFieldsToObject(deployObject, fieldsToAdd, valueGetter) {
    for (const fieldId of fieldsToAdd) {
      if (fieldId.type === 'object') {
        deployObject[fieldId.id] = this.copyFieldsToObject(
          deployObject[fieldId.id] || {},
          fieldId.values,
          (localFieldId) => this.getValueOrDefault(localFieldId));
      } else if (this.hasValue(valueGetter(fieldId.id))) {
        const value = valueGetter(fieldId.id);
        if (fieldId.type === 'text' || fieldId.type === 'array') {
          deployObject[fieldId.id] = value;
        } else if (fieldId.type === 'number') {
          deployObject[fieldId.id] = parseFloat(value, 10);
        } else if (fieldId.type === 'base64') {
          deployObject[fieldId.id] = btoa(value);
        } else if (fieldId.type === 'map') {
          const map = {};
          for (const element of value) {
            const split = element.split('=');
            if (split.length !== 2) {
              continue;
            }
            map[split[0]] = split[1];
          }
          if (map) {
            deployObject[fieldId.id] = map;
          }
        } else if (fieldId.type === 'artifacts') {
          const artifacts = value.map(artifact => {
            const newArtifact = {};
            this.copyFieldsToObject(newArtifact, ARTIFACT_FIELDS.all, (id) => artifact[id] || INDEXED_ALL_ARTIFACT_FIELDS[id].default);
            if (artifact.type === 'embedded') {
              this.copyFieldsToObject(newArtifact, ARTIFACT_FIELDS.embedded, (id) => artifact[id] || INDEXED_ALL_ARTIFACT_FIELDS[id].default);
            }
            if (artifact.type === 'external') {
              this.copyFieldsToObject(newArtifact, ARTIFACT_FIELDS.external, (id) => artifact[id] || INDEXED_ALL_ARTIFACT_FIELDS[id].default);
            }
            if (artifact.type === 's3') {
              this.copyFieldsToObject(newArtifact, ARTIFACT_FIELDS.s3, (id) => artifact[id] || INDEXED_ALL_ARTIFACT_FIELDS[id].default);
            }
            return newArtifact;
          });
          deployObject[fieldId.id] = artifacts;
        } else if (fieldId.type === 'mesosArtifacts') {
          const mesosArtifacts = value.map(mesosArtifact => this.copyFieldsToObject(
            {},
            MESOS_ARTIFACT_FIELDS,
            (id) => mesosArtifact[id] || INDEXED_MESOS_ARTIFACT_FIELDS[id].default
          ));
          deployObject[fieldId.id] = mesosArtifacts;
        } else if (fieldId.type === 'volumes') {
          const volumes = value.map(volume => this.copyFieldsToObject(
            {},
            DOCKER_VOLUME_FIELDS,
            (id) => volume[id] || INDEXED_DOCKER_VOLUME_FIELDS[id].default
          ));
          deployObject[fieldId.id] = volumes;
        } else if (fieldId.type === 'portMappings') {
          const portMappings = value.map(portMapping => this.copyFieldsToObject(
            {},
            DOCKER_PORT_MAPPING_FIELDS,
            (id) => portMapping[id] || INDEXED_DOCKER_PORT_MAPPING_FIELDS[id].default));
          deployObject[fieldId.id] = portMappings;
        } else if (fieldId.type === 'dockerParameters') {
          const dockerParameters = value.map(dockerParameter => this.copyFieldsToObject(
            {},
            DOCKER_PARAMETERS_FIELDS,
            (id) => dockerParameter[id] || INDEXED_DOCKER_PARAMETERS_FIELDS[id].default));
          deployObject[fieldId.id] = dockerParameters;
        }
      }
    }
    return deployObject;
  }

  submit(event) {
    event.preventDefault();
    const deployObject = {};
    this.copyFieldsToObject(deployObject, FIELDS.all, (fieldId) => this.getValueOrDefault(fieldId));
    if (this.getValueOrDefault('executorType') === DEFAULT_EXECUTOR_TYPE) {
      this.copyFieldsToObject(deployObject, FIELDS.defaultExecutor, (fieldId) => this.getValueOrDefault(fieldId));
    } else {
      this.copyFieldsToObject(deployObject, FIELDS.customExecutor, (fieldId) => this.getValueOrDefault(fieldId));
    }
    if (this.getValueOrDefault('type') === 'DOCKER') {
      this.copyFieldsToObject(deployObject, FIELDS.dockerContainer, (fieldId) => this.getValueOrDefault(fieldId));
    }
    if (this.props.request.request.loadBalanced) {
      this.copyFieldsToObject(deployObject, FIELDS.loadBalancer, (fieldId) => this.getValueOrDefault(fieldId));
    }
    if (this.isRequestDaemon()) {
      this.copyFieldsToObject(deployObject, FIELDS.healthChecker, (fieldId) => this.getValueOrDefault(fieldId));
    }
    deployObject.requestId = this.props.request.request.id;
    deployObject.shell = true;
    this.props.save({deploy: deployObject});
  }

  addObjectToArrayField(fieldId, obj) {
    if (!this.props.form[fieldId]) {
      this.updateField(fieldId, [obj]);
    } else {
      const fieldValue = this.props.form[fieldId].slice();
      fieldValue.push(obj);
      this.updateField(fieldId, fieldValue);
    }
  }

  addObjectToArrayFieldPreventDefault(fieldId, obj, event) {
    event.preventDefault();
    this.addObjectToArrayField(fieldId, obj);
  }

  removeObjectFromArrayField(fieldId, key) {
    const fieldValue = this.props.form[fieldId].slice();
    fieldValue.splice(key, 1);
    this.updateField(fieldId, fieldValue);
  }

  updateObjectInArrayField(fieldId, key, newFieldObj) {
    const newArray = this.props.form[fieldId].slice();
    const newValue = _.extend({}, newArray[key], newFieldObj);
    newArray[key] = newValue;
    this.updateField(fieldId, newArray);
  }

  renderMesosArtifact(mapping, key) {
    const thisMesosArtifact = this.props.form.uris[key];
    const uri = (
      <TextFormGroup
        id={`mesos-uri-${ key }`}
        onChange={event => this.updateObjectInArrayField('uris', key, {uri: event.target.value})}
        value={thisMesosArtifact.uri}
        label="Uri"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_MESOS_ARTIFACT_FIELDS.uri, thisMesosArtifact.uri)}
      />
    );
    const cache = (
      <CheckboxFormGroup
        id = "mesos-cahce-${ key }"
        label="Cache"
        checked = {thisMesosArtifact.cache}
        onChange={(newValue) => this.updateObjectInArrayField('uris', key, {cache: newValue})}
        feedback={this.formFieldFeedback(INDEXED_MESOS_ARTIFACT_FIELDS.cache, thisMesosArtifact.cache)}
      />
    );
    const extract = (
      <CheckboxFormGroup
        id = "mesos-extract-${ key }"
        label="Extract"
        checked = {thisMesosArtifact.extract}
        onChange={(newValue) => this.updateObjectInArrayField('uris', key, {extract: newValue})}
        feedback={this.formFieldFeedback(INDEXED_MESOS_ARTIFACT_FIELDS.extract, thisMesosArtifact.extract)}
      />
    );
    const executable = (
      <CheckboxFormGroup
        id = "mesos-executable-${ key }"
        label="Executable"
        checked = {thisMesosArtifact.executable}
        onChange={(newValue) => this.updateObjectInArrayField('uris', key, {executable: newValue})}
        feedback={this.formFieldFeedback(INDEXED_MESOS_ARTIFACT_FIELDS.executable, thisMesosArtifact.executable)}
      />
    );
    return (
      <div className="well well-sm mesos-artifact" key={key}>
        <h5>Mesos Artifact</h5>
        <button
          className="remove-button"
          id={`remove-mesos-artifact-${key}`}
          onClick={() => this.removeObjectFromArrayField('uris', key)}
        />
        {uri}
        {cache}
        {executable}
        {extract}
      </div>
    );
  }

  renderMesosArtifacts() {
    const mesosArtifacts = this.props.form.uris;
    if (mesosArtifacts) {
      return mesosArtifacts.map((mapping, key) => this.renderMesosArtifact(mapping, key));
    }
    return null;
  }

  renderDefaultExecutorFields() {
    const command = (
      <TextFormGroup
        id="command-to-execute"
        onChange={event => this.updateField('command', event.target.value)}
        value={this.props.form.command}
        label="Command to execute"
        placeholder="eg: rm -rf /"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.command, this.props.form.command)}
      />
    );
    const cmdLineArguments = (
      <MultiInputFormGroup
        id="cmd-line-args"
        value={this.props.form.arguments || []}
        onChange={(newValue) => this.updateField('arguments', newValue)}
        label="Arguments"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.arguments, () => this.props.form.arguments)}
        couldHaveFeedback={true}
      />
    );

    return (
      <div>
        <fieldset id="default-expandable" className="expandable">
          <h4>Default Executor Settings</h4>
          {command}
          {cmdLineArguments}
          {this.renderMesosArtifacts()}

          <div id="mesos-artifact-button-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('uris', {extract: true}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Artifact"}
              </button>
            </div>
          </div>
        </fieldset>
      </div>
    );
  }

  renderArtifact(artifact, key) {
    const arrayName = `${artifact.type}Artifacts`;
    const name = (
      <TextFormGroup
        id={`name-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {name: event.target.value})}
        value={artifact.name}
        label="Name"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.name, artifact.name)}
      />
    );
    const fileName = (
      <TextFormGroup
        id={`filename-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {filename: event.target.value})}
        value={artifact.filename}
        label="File name"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.filename, artifact.filename)}
      />
    );
    const md5Sum = (
      <TextFormGroup
        id={`md5-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {md5Sum: event.target.value})}
        value={artifact.md5Sum}
        label="MD5 checksum"
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.md5Sum, artifact.md5Sum)}
      />
    );
    const content = (
      <TextFormGroup
        id={`content-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {content: event.target.value})}
        value={artifact.content}
        label="Content"
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.content, artifact.content)}
      />
    );
    const filesize = (
      <TextFormGroup
        id={`file-size-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {filesize: event.target.value})}
        value={artifact.filesize}
        label="File size"
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.filesize, artifact.filesize)}
      />
    );
    const url = (
      <TextFormGroup
        id={`url-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {url: event.target.value})}
        value={artifact.url}
        label="URL"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.url, artifact.url)}
      />
    );
    const s3Bucket = (
      <TextFormGroup
        id={`bucket-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {s3Bucket: event.target.value})}
        value={artifact.s3Bucket}
        label="S3 bucket"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.s3Bucket, artifact.s3Bucket)}
      />
    );
    const s3ObjectKey = (
      <TextFormGroup
        id={`object-key-${ key }`}
        onChange={event => this.updateObjectInArrayField(arrayName, key, {s3ObjectKey: event.target.value})}
        value={artifact.s3ObjectKey}
        label="S3 object key"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_ARTIFACT_FIELDS.s3ObjectKey, artifact.s3ObjectKey)}
      />
    );
    return (
      <div key={key} className="well well-sm artifact">
        <h5>{artifact.type} artifact</h5>
        <button
          className="remove-button"
          id={`remove-artifact-${key}`}
          onClick={() => this.removeObjectFromArrayField(arrayName, key) }
        />
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
    if (this.props.form.s3Artifacts || this.props.form.externalArtifacts || this.props.form.embeddedArtifacts) {
      return (
        <div id="custom-artifacts">
          {this.props.form.embeddedArtifacts && this.props.form.embeddedArtifacts.map((artifact, key) => this.renderArtifact(artifact, key))}
          {this.props.form.externalArtifacts && this.props.form.externalArtifacts.map((artifact, key) => this.renderArtifact(artifact, key))}
          {this.props.form.s3Artifacts && this.props.form.s3Artifacts.map((artifact, key) => this.renderArtifact(artifact, key))}
        </div>
      );
    }
    return null;
  }

  renderCustomExecutorFields() {
    const command = (
      <TextFormGroup
        id="cmd-to-execute"
        onChange={event => this.updateField('cmd', event.target.value)}
        value={this.props.form.cmd}
        label="Command to execute"
        placeholder="eg: rm -rf /"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.cmd, this.props.form.cmd)}
      />
    );
    const customExecutorCmd = (
      <TextFormGroup
        id="custom-executor-command"
        onChange={event => this.updateField('customExecutorCmd', event.target.value)}
        value={this.props.form.customExecutorCmd}
        label="Custom executor command"
        required={true}
        placeholder="eg: /usr/local/bin/singularity-executor"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.customExecutorCmd, this.props.form.customExecutorCmd)}
      />
    );
    const extraCommandArgs = (
      <MultiInputFormGroup
        id="extra-args"
        value={this.props.form.extraCmdLineArgs || []}
        onChange={(newValue) => this.updateField('extraCmdLineArgs', newValue)}
        label="Extra command args"
        placeholder="eg: -jar MyThing.jar"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.extraCmdLineArgs, () => this.props.form.extraCmdLineArgs)}
        couldHaveFeedback={true}
      />
    );
    const user = (
      <TextFormGroup
        id="user"
        onChange={event => this.updateField('user', event.target.value)}
        value={this.props.form.user}
        label="User"
        placeholder="default: root"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.user, this.props.form.user)}
      />
    );
    const killAfterMillis = (
      <TextFormGroup
        id="kill-after-millis"
        onChange={event => this.updateField('sigKillProcessesAfterMillis', event.target.value)}
        value={this.props.form.sigKillProcessesAfterMillis}
        label="Kill processes after (milisec)"
        placeholder="default: 120000"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.sigKillProcessesAfterMillis, this.props.form.sigKillProcessesAfterMillis)}
      />
    );
    const successfulExitCodes = (
      <MultiInputFormGroup
        id="successful-exit-code"
        value={this.props.form.successfulExitCodes || []}
        onChange={(newValue) => this.updateField('successfulExitCodes', newValue)}
        label="Successful exit codes"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.successfulExitCodes, () => this.props.form.successfulExitCodes)}
        couldHaveFeedback={true}
      />
    );
    const maxTaskThreads = (
      <TextFormGroup
        id="max-task-threads"
        onChange={event => this.updateField('maxTaskThreads', event.target.value)}
        value={this.props.form.maxTaskThreads}
        label="Max Task Threads"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.maxTaskThreads, this.props.form.maxTaskThreads)}
      />
    );
    const loggingTag = (
      <TextFormGroup
        id="logging-tag"
        onChange={event => this.updateField('loggingTag', event.target.value)}
        value={this.props.form.loggingTag}
        label="Logging tag"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.loggingTag, this.props.form.loggingTag)}
      />
    );
    const loggingExtraFields = (
      <MultiInputFormGroup
        id="logging-extra-fields"
        value={this.props.form.loggingExtraFields || []}
        onChange={(newValue) => this.updateField('loggingExtraFields', newValue)}
        label="Logging extra fields"
        placeholder="format: key=value"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.loggingExtraFields, () => this.props.form.loggingExtraFields)}
        couldHaveFeedback={true}
      />
    );
    const logrotateFrequency = (
      <SelectFormGroup
        id="logrotate-frequency"
        label="Logrotate Frequency"
        value={this.props.form.logrotateFrequency}
        defaultValue="DAILY"
        onChange={(newValue) => this.updateField('logrotateFrequency', newValue.value)}
        required={false}
        options={[
          { label: 'Hourly', value: 'HOURLY' },
          { label: 'Daily', value: 'DAILY' },
          { label: 'Weekly', value: 'WEEKLY' },
          { label: 'Monthly', value: 'MONTHLY' }
        ]}
      />
    );
    const preserveSandbox = (
      <CheckboxFormGroup
        id = "preserve-sandbox"
        label="Preserve task sandbox after finish"
        checked = {this.props.form.preserveTaskSandboxAfterFinish}
        onChange = {(newValue) => this.updateField('preserveTaskSandboxAfterFinish', newValue)}
      />
    );
    const skipLogrotateAndCompress = (
      <CheckboxFormGroup
        id = "skip-lr-compress"
        label="Skip lorotate compress"
        checked = {this.props.form.skipLogrotateAndCompress}
        onChange = {(newValue) => this.updateField('skipLogrotateAndCompress', newValue)}
      />
    );
    const loggingS3Bucket = (
      <TextFormGroup
        id="logging-s3-bucket"
        onChange={event => this.updateField('loggingS3Bucket', event.target.value)}
        value={this.props.form.loggingS3Bucket}
        label="Logging S3 Bucket"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.loggingS3Bucket, this.props.form.loggingS3Bucket)}
      />
    );
    const maxOpenFiles = (
      <TextFormGroup
        id="max-open-files"
        onChange={event => this.updateField('maxOpenFiles', event.target.value)}
        value={this.props.form.maxOpenFiles}
        label="Max Open Files"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.maxOpenFiles, this.props.form.maxOpenFiles)}
      />
    );
    const runningSentinel = (
      <TextFormGroup
        id="running-sentinel"
        onChange={event => this.updateField('runningSentinel', event.target.value)}
        value={this.props.form.runningSentinel}
        label="Running Sentinel"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.runningSentinel, this.props.form.runningSentinel)}
      />
    );
    return (
      <div>
        <fieldset>
          <h4>Custom Executor Settingss</h4>
          {command}
          {customExecutorCmd}
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
              {logrotateFrequency}
            </div>
            <div className="col-md-6">
              {skipLogrotateAndCompress}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {preserveSandbox}
            </div>
            <div className="col-md-6">
              {loggingS3Bucket}
            </div>
          </div>

          <div className="row">
            <div className="col-md-6">
              {maxOpenFiles}
            </div>
            <div className="col-md-6">
              {runningSentinel}
            </div>
          </div>
        </fieldset>

        <fieldset>
          <h4>Custom executor artifacts</h4>

          { this.renderCustomArtifactFields() }

          <div id="artifact-button-row" className="row">
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('embeddedArtifacts', {type: 'embedded'}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Embedded"}
              </button>
            </div>
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('externalArtifacts', {type: 'external'}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" External"}
              </button>
            </div>
            <div className="col-sm-4">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('s3Artifacts', {type: 's3'}, event)}>
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
    const thisPortMapping = this.props.form.portMappings[key];
    const containerPortType = (
      <SelectFormGroup
        id={`cont-port-type-${ key }`}
        label="Container Port Type"
        value={thisPortMapping.containerPortType || INDEXED_DOCKER_PORT_MAPPING_FIELDS.containerPortType.default}
        defaultValue="LITERAL"
        onChange={newValue => this.updateObjectInArrayField('portMappings', key, {containerPortType: newValue.value})}
        required={true}
        options={[
          { label: 'Literal', value: 'LITERAL' },
          { label: 'From Offer', value: 'FROM_OFFER' }
        ]}
      />
    );
    const containerPort = (
      <TextFormGroup
        id={`cont-port-${ key }`}
        onChange={event => this.updateObjectInArrayField('portMappings', key, {containerPort: event.target.value})}
        value={thisPortMapping.containerPort}
        label="Container Port"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_DOCKER_PORT_MAPPING_FIELDS.containerPort, thisPortMapping.containerPort)}
      />
    );
    const hostPortType = (
      <SelectFormGroup
        id={`host-port-type-${ key }`}
        label="Host Port Type"
        value={thisPortMapping.hostPortType || INDEXED_DOCKER_PORT_MAPPING_FIELDS.hostPortType.default}
        defaultValue="LITERAL"
        onChange={newValue => this.updateObjectInArrayField('portMappings', key, {hostPortType: newValue.value})}
        required={true}
        options={[
          { label: 'Literal', value: 'LITERAL' },
          { label: 'From Offer', value: 'FROM_OFFER' }
        ]}
      />
    );
    const hostPort = (
      <TextFormGroup
        id={`host-port-${ key }`}
        onChange={event => this.updateObjectInArrayField('portMappings', key, {hostPort: event.target.value})}
        value={thisPortMapping.hostPort}
        label="Host Port"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_DOCKER_PORT_MAPPING_FIELDS.hostPort, thisPortMapping.hostPort)}
      />
    );
    const protocol = (
      <TextFormGroup
        id={`protocol-${ key }`}
        onChange={event => this.updateObjectInArrayField('portMappings', key, {protocol: event.target.value})}
        value={thisPortMapping.protocol}
        label="Protocol"
        placeholder="default: tcp"
        feedback={this.formFieldFeedback(INDEXED_DOCKER_PORT_MAPPING_FIELDS.protocol, thisPortMapping.protocol)}
      />
    );
    return (
      <div className="well well-sm docker-port" key={key}>
        <h5>Docker Port Mapping</h5>
        <button
          className="remove-button"
          id={`remove-port-mapping-${key}`}
          onClick={() => this.removeObjectFromArrayField('portMappings', key)}
        />
        {containerPortType}
        {containerPort}
        {hostPortType}
        {hostPort}
        {protocol}
      </div>
    );
  }

  renderDockerPortMappings() {
    const portMappings = this.props.form.portMappings;
    if (portMappings) {
      return portMappings.map((mapping, key) => this.renderDockerPortMapping(mapping, key));
    }
    return null;
  }

  renderDockerParameter(mapping, key) {
    const thisDockerParameter = this.props.form.dockerParameters[key];
    const keyValue = (
      <TextFormGroup
        id={`parameter-key-${ key }`}
        onChange={event => this.updateObjectInArrayField('dockerParameters', key, {key: event.target.value})}
        value={thisDockerParameter.key}
        label="Key"
        required={true}
      />
    );
    const realValue = (
      <TextFormGroup
        id={`parameter-value-${ key }`}
        onChange={event => this.updateObjectInArrayField('dockerParameters', key, {value: event.target.value})}
        value={thisDockerParameter.value}
        label="Value"
        required={false}
      />
    );
    return (
      <div className="well well-sm docker-port" key={key}>
        <h5>Docker Parameter</h5>
        <button
          className="remove-button"
          id={`remove-docker-parameter-${key}`}
          onClick={() => this.removeObjectFromArrayField('dockerParameters', key)}
        />
        {keyValue}
        {realValue}
      </div>
    );
  }

  renderDockerParameters() {
    const dockerParameters = this.props.form.dockerParameters;
    if (dockerParameters) {
      return dockerParameters.map((mapping, key) => this.renderDockerParameter(mapping, key));
    }
    return null;
  }

  renderDockerVolume(mapping, key) {
    const thisVolume = this.props.form.volumes[key];
    const containerPath = (
      <TextFormGroup
        id={`cont-path-${ key }`}
        onChange={event => this.updateObjectInArrayField('volumes', key, {containerPath: event.target.value})}
        value={thisVolume.containerPath}
        label="Container Path"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_DOCKER_VOLUME_FIELDS.containerPath, thisVolume.containerPath)}
      />
    );
    const hostPath = (
      <TextFormGroup
        id={`host-path-${ key }`}
        onChange={event => this.updateObjectInArrayField('volumes', key, {hostPath: event.target.value})}
        value={thisVolume.hostPath}
        label="Host Path"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_DOCKER_VOLUME_FIELDS.hostPath, thisVolume.hostPath)}
      />
    );
    const mode = (
      <SelectFormGroup
        id={`volume-mode-${ key }`}
        label="Volume Mode"
        value={thisVolume.mode || INDEXED_DOCKER_VOLUME_FIELDS.mode.default}
        defaultValue="RO"
        onChange={newValue => this.updateObjectInArrayField('volumes', key, {mode: newValue.value})}
        required={true}
        options={[
          { label: 'RO', value: 'RO' },
          { label: 'RW', value: 'RW' }
        ]}
      />
    );
    return (
      <div className="well well-sm docker-volume" key={key}>
        <h5>Docker Volume</h5>
        <button
          className="remove-button"
          id={`remove-volume-${key}`}
          onClick={() => this.removeObjectFromArrayField('volumes', key)}
        />
        {containerPath}
        {hostPath}
        {mode}
      </div>
    );
  }

  renderDockerVolumes() {
    const volumes = this.props.form.volumes;
    if (volumes) {
      return volumes.map((mapping, key) => this.renderDockerVolume(mapping, key));
    }
    return null;
  }

  renderDockerContainerFields() {
    const image = (
      <TextFormGroup
        id="docker"
        onChange={event => this.updateField('image', event.target.value)}
        value={this.props.form.image}
        label="Docker image"
        required={true}
        placeholder="eg: centos6:latest"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.image, this.props.form.image)}
      />
    );
    const network = (
      <SelectFormGroup
        id="dockernetwork"
        label="Docker Network"
        value={this.getValueOrDefault('network')}
        onChange={newValue => this.updateField('network', newValue.value)}
        options={[
          { label: 'None', value: 'NONE' },
          { label: 'Bridge', value: 'BRIDGE' },
          { label: 'Host', value: 'HOST' }
        ]}
      />
    );
    const privileged = (
      <CheckboxFormGroup
        id = "privileged"
        label="Privileged"
        checked = {this.props.form.privileged}
        onChange = {(newValue) => this.updateField('privileged', newValue)}
      />
    );
    const forcePullImage = (
      <CheckboxFormGroup
        id = "force-pull"
        label="Force Pull Image"
        checked = {this.props.form.forcePullImage}
        onChange = {(newValue) => this.updateField('forcePullImage', newValue)}
      />
    );
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

          {this.renderDockerParameters()}

          <div id="docker-parameter-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('dockerParameters', {}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Docker Parameter"}
              </button>
            </div>
          </div>

          {this.renderDockerPortMappings()}

          <div id="docker-port-button-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('portMappings', {}, event)}>
                <span className="glyphicon glyphicon-plus"></span>
                {" Docker Port Mapping"}
              </button>
            </div>
          </div>

          {this.renderDockerVolumes()}

          <div id="docker-volume-button-row" className="row">
            <div className="col-sm-6">
              <button className="btn btn-success btn-block" onClick={event => this.addObjectToArrayFieldPreventDefault('volumes', {}, event)}>
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
      <TextFormGroup
        id="id"
        onChange={event => this.updateField('id', event.target.value)}
        value={this.props.form.id}
        label="Deploy ID"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_FIELDS.id, this.props.form.id)}
      />
    );
    const executorType = (
      <SelectFormGroup
        id="executor-type"
        label="Executor type"
        value={this.getValueOrDefault('executorType')}
        onChange={newValue => this.updateField('executorType', newValue.value)}
        required={true}
        options={[
          { label: 'Default', value: DEFAULT_EXECUTOR_TYPE },
          { label: 'Custom', value: CUSTOM_EXECUTOR_TYPE }
        ]}
      />
    );
    const type = (
      <SelectFormGroup
        id="container-type"
        label="Container type"
        value={this.getValueOrDefault('type')}
        onChange={newValue => this.updateField('type', newValue.value)}
        required={true}
        options={[
          { label: 'Mesos', value: 'MESOS' },
          { label: 'Docker', value: 'DOCKER' }
        ]}
      />
    );
    const cpus = (
      <TextFormGroup
        id="cpus"
        onChange={event => this.updateField('cpus', event.target.value)}
        value={this.props.form.cpus}
        label="CPUs"
        placeholder={`default: ${config.defaultCpus}`}
        feedback={this.formFieldFeedback(INDEXED_FIELDS.cpus, this.props.form.cpus)}
      />
    );
    const memoryMb = (
      <TextFormGroup
        id="memory-mb"
        onChange={event => this.updateField('memoryMb', event.target.value)}
        value={this.props.form.memoryMb}
        label="Memory (MB)"
        placeholder={`default: ${config.defaultMemory}`}
        feedback={this.formFieldFeedback(INDEXED_FIELDS.memoryMb, this.props.form.memoryMb)}
      />
    );
    const numPorts = (
      <TextFormGroup
        id="cpus"
        onChange={event => this.updateField('numPorts', event.target.value)}
        value={this.props.form.numPorts}
        label="Num. ports"
        placeholder="default: 0"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.numPorts, this.props.form.numPorts)}
      />
    );
    const diskMb = (
      <TextFormGroup
        id="disk-mb"
        onChange={event => this.updateField('diskMb', event.target.value)}
        value={this.props.form.diskMb}
        label="Disk (MB)"
        placeholder={`default: ${config.defaultDisk}`}
        feedback={this.formFieldFeedback(INDEXED_FIELDS.diskMb, this.props.form.diskMb)}
      />
    );
    const env = (
      <MultiInputFormGroup
        id="env-vars"
        value={this.props.form.env || []}
        onChange={(newValue) => this.updateField('env', newValue)}
        placeholder="format: key=value"
        label="Environment variables"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.env, () => this.props.form.env)}
        couldHaveFeedback={true}
      />
    );
    const healthcheckUri = (
      <TextFormGroup
        id="healthcheck-uri"
        onChange={event => this.updateField('uri', event.target.value)}
        value={this.props.form.uri}
        label="Healthcheck URI"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.uri, this.props.form.uri)}
      />
    );
    const healthcheckPortIndex = (
      <TextFormGroup
        id="healthcheck-port-index"
        onChange={event => this.updateField('portIndex', event.target.value)}
        value={this.props.form.portIndex}
        label="HC Port Index"
        placeholder="default: 0 (first allocated port)"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.portIndex, this.props.form.portIndex)}
      />
    );
    const healthcheckPortNumber = (
      <TextFormGroup
        id="healthcheck-port-number"
        onChange={event => this.updateField('portNumber', event.target.value)}
        value={this.props.form.portNumber}
        label="HC Port Number"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.portNumber, this.props.form.portNumber)}
      />
    );
    const healthCheckProtocol = (
      <SelectFormGroup
        id="hc-protocol"
        label="HC Protocol"
        value={this.getValueOrDefault('protocol')}
        onChange={newValue => this.updateField('protocol', newValue.value)}
        options={[
          { label: 'HTTP', value: 'HTTP' },
          { label: 'HTTPS', value: 'HTTPS' }
        ]}
      />
    );
    const healthcheckStartupDelaySeconds = (
      <TextFormGroup
        id="healthcheck-startup-delay"
        onChange={event => this.updateField('startupDelaySeconds', event.target.value)}
        value={this.props.form.startupDelaySeconds}
        label="HC startup delay"
        placeholder="default: 0"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.startupDelaySeconds, this.props.form.startupDelaySeconds)}
      />
    );
    const healthcheckStartupIntervalSeconds = (
      <TextFormGroup
        id="healthcheck-startup-interval"
        onChange={event => this.updateField('startupIntervalSeconds', event.target.value)}
        value={this.props.form.startupIntervalSeconds}
        label="HC startup check interval"
        placeholder="default: 5"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.startupIntervalSeconds, this.props.form.startupIntervalSeconds)}
      />
    );
    const healthcheckStartupTimeoutSeconds = (
      <TextFormGroup
        id="healthcheck-startup-timeout"
        onChange={event => this.updateField('startupTimeoutSeconds', event.target.value)}
        value={this.props.form.startupTimeoutSeconds}
        label="HC startup timeout"
        placeholder="default: 30"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.startupTimeoutSeconds, this.props.form.startupTimeoutSeconds)}
      />
    );
    const healthcheckTimeoutSeconds = (
      <TextFormGroup
        id="healthcheck-timeout"
        onChange={event => this.updateField('responseTimeoutSeconds', event.target.value)}
        value={this.props.form.responseTimeoutSeconds}
        label="HC response timeout (sec)"
        placeholder="default: 5"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.responseTimeoutSeconds, this.props.form.responseTimeoutSeconds)}
      />
    );
    const healthcheckIntervalSeconds = (
      <TextFormGroup
        id="healthcheck-interval"
        onChange={event => this.updateField('intervalSeconds', event.target.value)}
        value={this.props.form.intervalSeconds}
        label="HC interval (sec)"
        placeholder="default: 5"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.intervalSeconds, this.props.form.intervalSeconds)}
      />
    );
    const healthcheckMaxRetries = (
      <TextFormGroup
        id="healthcheck-max-retries"
        onChange={event => this.updateField('maxRetries', event.target.value)}
        value={this.props.form.maxRetries}
        label="HC Max Retries"
        placeholder="default: 0 (no retries)"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.maxRetries, this.props.form.maxRetries)}
      />
    );
    const failureStatusCodes = (
      <MultiInputFormGroup
        id="hc-failure-status-codes"
        value={this.props.form.failureStatusCodes || []}
        onChange={(newValue) => this.updateField('failureStatusCodes', newValue)}
        label="HC failureStatusCodes"
        required={false}
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.failureStatusCodes, () => this.props.form.failureStatusCodes)}
        couldHaveFeedback={true}
      />
    );
    const skipHealthchecksOnDeploy = (
      <CheckboxFormGroup
        id = "skip-healthcheck"
        label="Skip healthcheck on deploy"
        checked = {this.props.form.skipHealthchecksOnDeploy}
        onChange = {(newValue) => this.updateField('skipHealthchecksOnDeploy', newValue)}
      />
    );
    const deployHealthTimeoutSeconds = (
      <TextFormGroup
        id="deploy-healthcheck-timeout"
        onChange={event => this.updateField('deployHealthTimeoutSeconds', event.target.value)}
        value={this.props.form.deployHealthTimeoutSeconds}
        label="Deploy healthcheck timeout (sec)"
        placeholder="default: 120"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.deployHealthTimeoutSeconds, this.props.form.deployHealthTimeoutSeconds)}
      />
    );
    const considerHealthyAfterRunningForSeconds = (
      <TextFormGroup
        id="consider-healthy-after"
        onChange={event => this.updateField('considerHealthyAfterRunningForSeconds', event.target.value)}
        value={this.props.form.considerHealthyAfterRunningForSeconds}
        label="Consider Healthy After Running For (sec)"
        placeholder="default: 5"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.considerHealthyAfterRunningForSeconds, this.props.form.considerHealthyAfterRunningForSeconds)}
      />
    );
    const serviceBasePath = (
      <TextFormGroup
        id="service-base-path"
        onChange={event => this.updateField('serviceBasePath', event.target.value)}
        value={this.props.form.serviceBasePath}
        label="Service base path"
        placeholder="eg: /singularity/api/v2"
        required={true}
        feedback={this.formFieldFeedback(INDEXED_FIELDS.serviceBasePath, this.props.form.serviceBasePath)}
      />
    );
    const loadBalancerGroups = (
      <MultiInputFormGroup
        id="lb-group"
        value={this.props.form.loadBalancerGroups || []}
        onChange={(newValue) => this.updateField('loadBalancerGroups', newValue)}
        label="Load balancer groups"
        required={true}
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.loadBalancerGroups, () => this.props.form.loadBalancerGroups)}
        couldHaveFeedback={true}
      />
    );
    const loadBalancerOptions = (
      <MultiInputFormGroup
        id="lb-option"
        value={this.props.form.loadBalancerOptions || []}
        onChange={(newValue) => this.updateField('loadBalancerOptions', newValue)}
        label="Load balancer options"
        placeholder="format: key=value"
        errorIndices={this.errorsInArrayField(INDEXED_FIELDS.loadBalancerOptions, () => this.props.form.loadBalancerOptions)}
        couldHaveFeedback={true}
      />
    );
    const loadBalancerPortIndex = (
      <TextFormGroup
        id="lb-port-index"
        onChange={event => this.updateField('loadBalancerPortIndex', event.target.value)}
        value={this.props.form.loadBalancerPortIndex}
        label="Load balancer port index"
        placeholder="default: 0 (first allocated port)"
        feedback={this.formFieldFeedback(INDEXED_FIELDS.loadBalancerPortIndex, this.props.form.loadBalancerPortIndex)}
      />
    );
    const unpauseOnSuccessfulDeploy = (
      <CheckboxFormGroup
        id = "deploy-to-unpause"
        label="Unpause on successful deploy"
        checked = {this.props.form.unpauseOnSuccessfulDeploy}
        onChange = {(newValue) => this.updateField('unpauseOnSuccessfulDeploy', newValue)}
      />
    );

    // Groups
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
        { this.getValueOrDefault('executorType') === DEFAULT_EXECUTOR_TYPE && this.renderDefaultExecutorFields() }
        { this.getValueOrDefault('executorType') === CUSTOM_EXECUTOR_TYPE && this.renderCustomExecutorFields() }
      </div>
    );
    const containerInfo = (
      <div className="well">
        <div className="row">
          <div className="col-md-4">
            <h3>Container Info</h3>
          </div>
          <div className="col-md-8">
            {type}
          </div>
        </div>

        { this.getValueOrDefault('type') === 'DOCKER' && this.renderDockerContainerFields() }
      </div>
    );
    const resources = (
      <div className="well">
        <h3>Resources</h3>
        <fieldset>
          <div className="row">
            <div className="col-sm-4">
              {cpus}
            </div>

            <div className="col-sm-4">
              {memoryMb}
            </div>

            <div className="col-sm-4">
              {numPorts}
            </div>
          </div>
          <div className="row">
            {config.showTaskDiskResource &&
              <div className="col-sm-4">
                {diskMb}
              </div>
            }
          </div>
        </fieldset>
      </div>
    );
    const variables = (
      <div className="well">
        <h3>Variables</h3>
        <fieldset>
          {env}
        </fieldset>
      </div>
    );
    const health = (
      <div className="well">
        <h3>Deploy Health</h3>
        <fieldset>
          {this.props.request.request.requestType === 'SERVICE' &&
            <div>
              {healthcheckUri}
              <div className="row">
                <div className="col-md-6">
                  {healthcheckPortIndex}
                </div>
                <div className="col-md-6">
                  {healthcheckPortNumber}
                </div>
              </div>
              <div className="row">
                <div className="col-md-6">
                  {healthCheckProtocol}
                </div>
                <div className="col-md-6">
                  {healthcheckStartupDelaySeconds}
                </div>
              </div>
              <div className="row">
                <div className="col-md-6">
                  {healthcheckStartupTimeoutSeconds}
                </div>
                <div className="col-md-6">
                  {healthcheckStartupIntervalSeconds}
                </div>
              </div>
              <div className="row">
                <div className="col-md-6">
                  {healthcheckTimeoutSeconds}
                </div>
                <div className="col-md-6">
                  {healthcheckIntervalSeconds}
                </div>
              </div>
              <div className="row">
                <div className="col-md-6">
                  {healthcheckMaxRetries}
                </div>
                <div className="col-md-6">
                  {failureStatusCodes}
                </div>
              </div>
              <div className="row">
                <div className="col-md-6">
                  {deployHealthTimeoutSeconds}
                </div>
                <div className="col-md-6">
                  {skipHealthchecksOnDeploy}
                </div>
              </div>
            </div>}
          {this.props.request.request.requestType !== 'SERVICE' && considerHealthyAfterRunningForSeconds}
        </fieldset>
      </div>
    );
    const loadBalancer = (
      <div className="well">
        <h3>Load Balancer</h3>
        <fieldset>
          {serviceBasePath}
          {loadBalancerGroups}
          {loadBalancerOptions}
          {loadBalancerPortIndex}
        </fieldset>
      </div>
    );
    const unpause = (
      <div className="well">
        <h3>Unpause</h3>
        <fieldset>
          {unpauseOnSuccessfulDeploy}
        </fieldset>
      </div>
    );

    const errorMessage = (
      this.props.saveApiCall.error &&
        <p className="alert alert-danger">
          There was a problem saving your deploy: {this.props.saveApiCall.error}
        </p> ||
        this.props.saveApiCall.data && this.props.saveApiCall.data.message &&
        <p className="alert alert-danger">
          There was a problem saving your deploy: {this.props.saveApiCall.data.message}
        </p>
    );
    const successMessage = (
      this.props.saveApiCall.data.activeDeploy &&
        <p className="alert alert-success">
          Deploy
          <Link
            to={`request/${ this.props.saveApiCall.data.activeDeploy.requestId }/deploy/${ this.props.saveApiCall.data.activeDeploy.id }`}
            >
            {` ${this.props.saveApiCall.data.activeDeploy.id} `}
          </Link>
          succesfully created!
        </p> || this.props.saveApiCall.data.pendingDeploy &&
        <p className="alert alert-success">
          Deploy
          <Link
            to={`request/${ this.props.saveApiCall.data.pendingDeploy.requestId }/deploy/${ this.props.saveApiCall.data.pendingDeploy.id }`}
            >
            {` ${this.props.saveApiCall.data.pendingDeploy.id} `}
          </Link>
          succesfully created!
        </p>
    );

    return (
      <div>
        <h2>
          New deploy for <Link to={`request/${ this.props.request.request.id }`}>{ this.props.request.request.id }</Link>
        </h2>
        <div className="row new-form">
          <form className="col-md-8" role="form" onSubmit={event => this.submit(event)}>

            {deployId}
            {executorInfo}
            {containerInfo}
            {resources}
            {variables}
            {this.isRequestDaemon() && health}
            {this.isRequestDaemon() && this.props.request.request.loadBalanced && loadBalancer}
            {this.props.request.state === 'PAUSED' && unpause}

            <div id="button-row">
              <span>
                <button type="submit" className="btn btn-success btn-lg" disabled={!this.canSubmit()}>
                  Deploy
                </button>
              </span>
            </div>

            {errorMessage || successMessage}

          </form>
          <div id="help-column" className="col-md-4 col-md-offset-1" />
        </div>
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    request: Utils.maybe(state.api.request, [ownProps.params.requestId, 'data']),
    notFound: Utils.maybe(state.api.request, [ownProps.params.requestId, 'statusCode']) === 404,
    pathname: ownProps.location.pathname,
    form: state.ui.form[FORM_ID],
    saveApiCall: state.api.saveDeploy
  };
}

function mapDispatchToProps(dispatch, ownProps) {
  return {
    update(formId, fieldId, newValue) {
      dispatch(ModifyField(formId, fieldId, newValue));
    },
    save(deployBody) {
      dispatch(SaveDeploy.trigger(deployBody)).then((response) => {
        if (response.type === 'SAVE_DEPLOY_SUCCESS') {
          ownProps.router.push(`request/${ownProps.params.requestId}/deploy/${response.data.pendingDeployState.deployMarker.deployId}`);
        }
      });
    },
    fetchRequest(requestId) {
      return dispatch(FetchRequest.trigger(requestId, true));
    },
    clearForm() {
      return dispatch(ClearForm(FORM_ID));
    },
    clearSaveDeployData() {
      return dispatch(SaveDeploy.clearData());
    }
  };
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(rootComponent(NewDeployForm, (props) => refresh(props.params.requestId, FORM_ID))));
