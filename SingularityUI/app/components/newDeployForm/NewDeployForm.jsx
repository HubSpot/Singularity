import React, {Component} from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import MultiInput from '../common/formItems/MultiInput';
import DropDown from '../common/formItems/DropDown';
import CheckBox from '../common/formItems/CheckBox';
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
  ],
  DEFAULT_EXECUTOR: [
    'command',
    'uris'
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
        'skipLogrotateAndCompress'
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

  getExecutorType() {
    return this.getValue('executorType') ? this.getValue('executorType') : CUSTOM_EXECUTOR_TYPE//DEFAULT_EXECUTOR_TYPE
  }

  render() {
    return (
      <div>
        <h2>
          New deploy for <a href={`${ config.appRoot }/request/${ this.props.request.id }`}>{ this.props.request.id }</a>
        </h2>
        <div className="row new-form">
          <form className="col-md-8">
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
            <div className="well">
              <div className="row">
                <div className="col-md-4">
                    <h3>Executor Info</h3>
                </div>
                <div className="col-md-8">
                    <div className="form-group required">
                        <label htmlFor="executor-type">Executor type</label>
                        <DropDown
                          id = 'executor-type'
                          prop = {{
                            updateFn: event => this.updateField('executorType', event.target.value),
                            forceChooseValue: true,
                            choices: [
                              {
                                value: DEFAULT_EXECUTOR_TYPE,
                                user: 'Default'
                              },
                              {
                                value: CUSTOM_EXECUTOR_TYPE,
                                user: 'Custom'
                              }
                            ],
                            value: this.getExecutorType()
                          }}
                        />
                    </div>
                </div>
              </div>
              <div className="form-group">
                <label htmlFor="command">Command to execute</label>
                <FormField
                  id = "command"
                  prop = {{
                    updateFn: event => this.updateField("command", event.target.value),
                    inputType: 'text',
                    value: this.getValue("command"),
                    required: true,
                    placeholder: "eg: rm -rf /"
                  }}
                />
              </div>
              {
                this.getExecutorType() === DEFAULT_EXECUTOR_TYPE ?
                  <div>
                    <fieldset id="default-expandable" className='expandable'>
                      <h4>Default Executor Settings</h4>

                      <label htmlFor="cmd-line-args">Arguments</label>
                      <MultiInput
                        id = "cmd-line-args"
                        value = {this.getValue('arguments') || []}
                        onChange = {(newValue) => this.updateField('arguments', newValue)}
                      />

                      <label htmlFor="artifacts" >Artifacts</label>
                      <MultiInput
                        id = "artifacts"
                        value = {this.getValue('uris') || []}
                        onChange = {(newValue) => this.updateField('uris', newValue)}
                        placeholder="eg: http://s3.example/my-artifact"
                      />
                    </fieldset>
                  </div> :
                  null
              }
              {
                this.getExecutorType() === CUSTOM_EXECUTOR_TYPE ?
                <div>
                  <fieldset>
                    <h4>Custom Executor Settingss</h4>

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

                    <div className="form-group">
                      <label htmlFor="extra-args">Extra command args</label>
                      <MultiInput
                        id = "extra-args"
                        value = {this.getValue('extraCmdLineArgs') || []}
                        onChange = {(newValue) => this.updateField('extraCmdLineArgs', newValue)}
                        placeholder="eg: -jar MyThing.jar"
                      />
                    </div>

                    <div className="row">
                      <div className="col-md-6">
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
                      </div>
                      <div className="col-md-6">
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
                      </div>
                    </div>

                    <div className="row">
                      <div className="col-md-6">
                        <div className="form-group">
                          <label htmlFor="successful-exit-code">Successful exit codes</label>
                          <MultiInput
                            id = "successful-exit-code"
                            value = {this.getValue("successfulExitCodes") || []}
                            onChange = {(newValue) => this.updateField("successfulExitCodes", newValue)}
                          />
                        </div>
                      </div>
                      <div className="col-md-6">
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
                      </div>
                    </div>

                    <div className="row">
                      <div className="col-md-6">
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
                      </div>
                      <div className="col-md-6">
                        <div className="form-group">
                          <label htmlFor="logging-extra-fields">Logging extra fields</label>
                          <MultiInput
                            id = "logging-extra-fields"
                            value = {this.getValue("loggingExtraFields") || []}
                            onChange = {(newValue) => this.updateField("loggingExtraFields", newValue)}
                            placeholder="format: key=value"
                          />
                        </div>
                      </div>
                    </div>

                    <div className="row">
                      <div className="col-md-6">
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
                      </div>
                      <div className="col-md-6">
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
                      </div>
                    </div>

                    <div className="row">
                      <div className="col-md-6">
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
                      </div>
                      <div className="col-md-6">
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
                      </div>
                    </div>

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

                  </fieldset>
                </div> :
                null
              }
            </div>
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

export default connect(mapStateToProps, mapDispatchToProps, undefined, {pure: false})(NewDeployForm);
