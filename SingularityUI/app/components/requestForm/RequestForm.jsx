import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import CheckBox from '../common/formItems/CheckBox';
import { modifyField, clearForm } from '../../actions/form';
import {makeSaveAction} from '../../actions/api/request';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';
import classNames from 'classnames';

let FORM_ID = 'requestForm';

let REQUEST_TYPES = ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];

let FIELDS_BY_REQUEST_TYPE = {
  ALL: [
    'id',
    'owners',
    'requestType',
    'slavePlacement'
  ],
  SERVICE: [
    'instances',
    'rackSensitive',
    'hideEvenNumberAcrossRacksHint',
    'loadBalanced',
    'rackAffinity'
  ],
  WORKER: [
    'instances',
    'rackSensitive',
    'hideEvenNumberAcrossRacksHint',
    'waitAtLeastMillisAfterTaskFinishesForReschedule',
    'rackAffinity'
  ],
  SCHEDULED: [
    'quartzSchedule',
    'cronSchedule',
    'scheduleType',
    'numRetriesOnFailure',
    'killOldNonLongRunningTasksAfterMillis',
    'scheduledExpectedRuntimeMillis'
  ],
  ON_DEMAND: [ 'killOldNonLongRunningTasksAfterMillis' ],
  RUN_ONCE: [ 'killOldNonLongRunningTasksAfterMillis' ]
}

class RequestForm extends React.Component {

  componentDidMount() {
    this.props.clearForm(FORM_ID);
  }

  isEditing() {
    return this.props.edit && this.props.request && this.props.request.request;
  }

  getValue(fieldId) {
    if (this.props.form && this.props.form[fieldId] !== undefined) {
      return this.props.form[fieldId];
    } else if (this.isEditing() && this.props.request.request[fieldId] !== undefined) {
      return this.props.request.request[fieldId];
    } else {
      return ""
    }
  }

  cantSubmit() {
    if (!this.getValue('id')) {
      return true;
    }
    if (!this.getValue('requestType')) {
      return true;
    }
    if (this.getValue('requestType') === 'SCHEDULED' && !this.getValue(this.getScheduleType())) {
      return true;
    }
    return false;
  }

  submitForm(event) {
    event.preventDefault();
    let request = {};
    let copyOverField = (fieldId) => {
      if (this.getValue(fieldId)) {
        request[fieldId] = this.getValue(fieldId);
      }
    }
    
    FIELDS_BY_REQUEST_TYPE[this.getValue('requestType')].map(copyOverField)
    FIELDS_BY_REQUEST_TYPE.ALL.map(copyOverField)

    if (this.getValue('scheduleType') === 'quartzSchedule') {
      request.schedule = ''
    }

    if (['ON_DEMAND', 'RUN_ONCE'].indexOf(this.getValue('requestType')) !== -1) {
      request.daemon = false
    } else if (['SERVICE', 'WORKER'].indexOf(this.getValue('requestType')) !== -1) {
      request.daemon = true
    }

    if (request.owners) {
      request.owners = request.owners.split(',')
    }
    if (request.rackAffinity) {
      request.rackAffinity = request.rackAffinity.split(',')
    }

    this.props.save(request);
    return null;
  }

  shouldRenderField(fieldId) {
    if (!this.getValue('requestType')) {
      return false;
    } else if (FIELDS_BY_REQUEST_TYPE[this.getValue('requestType')].indexOf(fieldId) === -1) {
      return false;
    } else {
      return true;
    }
  }

  getButtonsDisabled(type) {
    if (this.isEditing() && this.getValue('requestType') !== type) {
      return 'disabled';
    } else {
      return null;
    }
  }

  updateField(fieldId, newValue) {
    this.props.update(FORM_ID, fieldId, newValue)
  }

  updateTypeButtonClick(event) {
    event.preventDefault();
    this.updateField('requestType', event.target.value);
  }

  getScheduleType() {
    if (this.isEditing() && !(this.props.form && this.props.form.scheduleType)) {
      if (this.props.request.request.quartzSchedule) {
        return 'quartzSchedule';
      } else {
        return 'chronSchedule';
      }
    } else {
      if (this.props.form && this.props.form.scheduleType) {
        return this.props.form.scheduleType;
      } else {
        return 'chronSchedule';
      }
    }
  }

  renderRequestTypeSelectors() {
    let selectors = [];
    let tooltip = (
      <ToolTip id="cannotChangeRequestTypeAfterCreation">Option cannot be altered after creation</ToolTip>
    );
    REQUEST_TYPES.map((requestType, key) => {
      let selector = (
        <button
          key={key}
          value={requestType}
          className={classNames('btn', 'btn-default', {active: this.getValue('requestType') === requestType})}
          onClick={event => this.updateTypeButtonClick(event)}
          disabled={this.getButtonsDisabled(requestType)}
        >
          {Utils.humanizeText(requestType)}
        </button>
      );
      if (this.isEditing() && requestType === this.getValue('requestType')) {
        selectors.push (<OverlayTrigger placement="top" key={key} overlay={tooltip}>{selector}</OverlayTrigger>);
      } else {
        selectors.push (selector);
      }
    })
    return selectors;
  }

  renderLoadBalanced() {
    let checkbox = (
      <label htmlFor="load-balanced" className={classNames({subtle: this.isEditing()})}>
        <CheckBox
          id = "load-balanced"
          onChange = {event => this.updateField("loadBalanced", !this.getValue("loadBalanced"))}
          checked = {this.getValue("loadBalanced")}
          disabled = {this.isEditing()}
          noFormControlClass = {true}
        />
        {" Load balanced"}
      </label>
    );
    let field;
    if (this.isEditing()) {
      field = (
        <OverlayTrigger
          placement="top"
          overlay = {<ToolTip id="cannotChangeLoadBalancedAfterCreation">Option cannot be altered after creation</ToolTip>}>
          {checkbox}
        </OverlayTrigger>
      );
    } else {
      field = checkbox;
    }
    return (
      <div className = 'form-group'>
        {field}
      </div>
    );
  }

  renderRequestTypeSpecificFormFields() {
    return (
      <div>
        {
          this.shouldRenderField('instances') ?
            <div className='form-group'>
              <label htmlFor='instances'>Instances</label>
              <FormField
                id = 'instances'
                prop = {{
                  updateFn: event => this.updateField('instances', event.target.value),
                  placeholder: "1",
                  inputType: 'text',
                  value: this.getValue('instances')
                }}
              />
            </div> :
            undefined
        }
        {
          this.shouldRenderField('rackSensitive') ?
            <div className="form-group">
              <label htmlFor="rack-sensitive">
                <CheckBox
                  id = "rack-sensitive"
                  onChange = {event => this.updateField("rackSensitive", !this.getValue("rackSensitive"))}
                  checked = {this.getValue("rackSensitive")}
                  noFormControlClass = {true}
                />
                {" Rack Sensitive"}
              </label>
            </div> :
            undefined
        }
        {
          this.shouldRenderField('hideEvenNumberAcrossRacksHint') ?
            <div className='form-group'>
              <label htmlFor="hide-distribute-evenly-across-racks-hint">
                <CheckBox
                  id = "hide-distribute-evenly-across-racks-hint"
                  onChange = {event => this.updateField("hideEvenNumberAcrossRacksHint", !this.getValue("hideEvenNumberAcrossRacksHint"))}
                  checked = {this.getValue("hideEvenNumberAcrossRacksHint")}
                  noFormControlClass = {true}
                />
                {" Hide Distribute Evenly Across Racks Hint"}
              </label>
            </div> :
            undefined
        }
        { this.shouldRenderField('loadBalanced') ? this.renderLoadBalanced() : undefined }
        {
          this.shouldRenderField('waitAtLeastMillisAfterTaskFinishesForReschedule') ?
            <div className='form-group'>
              <label htmlFor='waitAtLeast'>Task rescheduling delay</label>
              <div className="input-group">
                <FormField
                  id = 'waitAtLeast'
                  prop = {{
                    updateFn: event => this.updateField('waitAtLeastMillisAfterTaskFinishesForReschedule', event.target.value),
                    inputType: 'text',
                    value: this.getValue('waitAtLeastMillisAfterTaskFinishesForReschedule')
                  }}
                />
                <div className="input-group-addon">milliseconds</div>
              </div>
            </div> :
            undefined
        }
        {
          this.shouldRenderField('rackAffinity') ?
            <div className='form-group'>
              <label htmlFor="rack-affinity">Rack Affinity <span className='form-label-tip'>separate multiple racks with commas</span></label>
              <FormField
                id = "rack-affinity"
                prop = {{
                  updateFn: event => this.updateField('rackAffinity', event.target.value),
                  inputType: 'text',
                  value: this.getValue('rackAffinity'),
                  generateSelectBox: true,
                  selectBoxOptions: {
                    tags: _.pluck(this.props.racks, 'id'),
                    selectOnBlur: true,
                    tokenSeparators: [',',' ']
                  }
                }}
              />
            </div> :
            undefined
        }
        {
          this.shouldRenderField('cronSchedule') || this.shouldRenderField('quartzSchedule') ?
            <div className='form-group required'>
              <label htmlFor='schedule'>Schedule</label>
              <div className="input-group">
                <FormField
                  id = 'schedule'
                  prop = {{
                    updateFn: event => this.updateField(this.getScheduleType(), event.target.value),
                    placeholder: this.getScheduleType() === 'quartzSchedule' ? "eg: 0 */5 * * * ?" : "eg: */5 * * * *",
                    inputType: 'text',
                    value: this.getValue(this.getScheduleType())
                  }}
                />
                <div className="input-group-addon input-group-addon--select">
                  <DropDown
                    id = 'schedule-type'
                    prop = {{
                      updateFn: event => this.updateField('scheduleType', event.target.value),
                      forceChooseValue: true,
                      choices: [
                        {
                          value: 'cronSchedule',
                          user: 'Cron Schedule'
                        },
                        {
                          value: 'quartzSchedule',
                          user: 'Quartz Schedule'
                        }
                      ],
                      value: this.getValue('scheduleType') ? this.getValue('scheduleType') : this.getScheduleType(),
                      generateSelectBox: true,
                      selectBoxOptions: {containerCssClass : "select2-select-box select-box-small"}
                    }}
                  />
                </div>
              </div>
            </div> :
            undefined
        }
        {
          this.shouldRenderField('numRetriesOnFailure') ?
            <div className='form-group'>
              <label htmlFor='retries-on-failure'>Number of retries on failure</label>
              <FormField
                id = 'retries-on-failure'
                prop = {{
                  updateFn: event => this.updateField('numRetriesOnFailure', event.target.value),
                  inputType: 'text',
                  value: this.getValue('numRetriesOnFailure')
                }}
              />
            </div> :
            undefined
        }
        {
          this.shouldRenderField('killOldNonLongRunningTasksAfterMillis') ?
            <div className='form-group'>
              <label htmlFor='killOldNRL'>Kill cleaning task(s) after</label>
              <div className="input-group">
                <FormField
                  id = 'killOldNRL'
                  prop = {{
                    updateFn: event => this.updateField('killOldNonLongRunningTasksAfterMillis', event.target.value),
                    inputType: 'text',
                    value: this.getValue('killOldNonLongRunningTasksAfterMillis')
                  }}
                />
                <div className="input-group-addon">milliseconds</div>
              </div>
            </div> :
            undefined
        }
        {
          this.shouldRenderField('scheduledExpectedRuntimeMillis') ?
            <div className='form-group'>
              <label htmlFor='expected-runtime'>Maximum task duration</label>
              <div className="input-group">
                <FormField
                  id = 'expected-runtime'
                  prop = {{
                    updateFn: event => this.updateField('scheduledExpectedRuntimeMillis', event.target.value),
                    inputType: 'text',
                    value: this.getValue('scheduledExpectedRuntimeMillis')
                  }}
                />
                <div className="input-group-addon">milliseconds</div>
              </div>
            </div> :
            undefined
        }
      </div>
    );
  }

  render() {
    let requestId = this.isEditing() ? this.props.request.request.id : undefined;
    let labelTip = (<span className='form-label-tip'>separate multiple owners with commas</span>);
    return (
      <div className="row new-form">
        <div className="col-md-5 col-md-offset-3">
          {this.isEditing() ?
            <h3>
              Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a>
            </h3> :
            <h3>New Request</h3>
          }
          <form role='form' onSubmit={event => this.submitForm(event)}>
            {this.isEditing() ?
              undefined :
              <div className="form-group required">
                <label htmlFor="id">ID</label>
                <FormField
                  id = "id"
                  prop = {{
                    updateFn: event => {
                      this.updateField("id", event.target.value);
                    },
                    placeholder: "eg: my-awesome-request",
                    inputType: 'text',
                    value: this.getValue("id"),
                    required: true
                  }}
                />
              </div>
            }
            <div className="form-group">
              <label htmlFor='owners'>Owners <span className='form-label-tip'>separate multiple owners with commas</span></label>
              <FormField
                id = "owners"
                prop = {{
                  updateFn: event => {
                    this.updateField('owners', event.target.value);
                  },
                  inputType: 'text',
                  value: this.getValue('owners'),
                  generateSelectBox: true,
                  selectBoxOptions: {
                    tags: [],
                    containerCssClass: 'select-owners hide-select2-spinner',
                    dropdownCssClass: 'hidden',
                    selectOnBlur: true,
                    tokenSeparators: [',',' ']
                  }
                }}
              />
            </div>
            <div className="form-group">
              <label>Type</label>
              <div id="type" class="btn-group">
                {this.renderRequestTypeSelectors()}
              </div>
            </div>
            {this.isEditing() ?
              <div className="alert alert-info alert-slim" role="alert">
                <strong>Note:</strong> changes made below will only affect new tasks
              </div> :
              undefined}
            <div className="form-group slavePlacement">
              <label htmlFor="slavePlacement" className="control-label">
                Slave Placement
              </label>
              <DropDown
                id = 'slavePlacement'
                prop = {{
                  updateFn: event => {
                    this.updateField('slavePlacement', event.target.value);
                  },
                  forceChooseValue: true,
                  choices: [
                    {
                      value: "",
                      user: "Default"
                    },
                    {
                      value: "SEPARATE",
                      user: "Separate"
                    },
                    {
                      value: "OPTIMISTIC",
                      user: "Optimistic"
                    },
                    {
                      value: "GREEDY",
                      user: "Greedy"
                    }
                  ],
                  value: this.getValue('slavePlacement') ? this.getValue('slavePlacement') : ""
                }}
              />
            </div>
            {this.renderRequestTypeSpecificFormFields()}
            <div id="button-row">
              <span>
                <button type="submit" className="btn btn-success btn-lg" disabled={this.cantSubmit() ? 'disabled' : undefined}>
                  Save
                </button>
              </span>
            </div>
          </form>
        </div>
      </div>
    );
  }

};

function mapStateToProps(state) {
  return {
    racks: state.api.racks.data,
    request: state.api.request ? state.api.request.data : undefined,
    form: state.form[FORM_ID]
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
      dispatch(makeSaveAction(requestBody).trigger());
    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps, undefined, {pure: false})(RequestForm);
