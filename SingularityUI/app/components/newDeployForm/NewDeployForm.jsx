import React from 'react';
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

class NewDeployForm extends React.Component {

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
    return this.getValue('executorType') ? this.getValue('executorType') : DEFAULT_EXECUTOR_TYPE
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
