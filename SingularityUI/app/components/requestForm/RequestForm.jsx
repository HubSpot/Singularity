import React from 'react';
import Select from 'react-select';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import MultiSelect from '../common/formItems/MultiSelect';
import CheckBox from '../common/formItems/CheckBox';
import { ModifyField, ClearForm } from '../../actions/ui/form';
import { SaveRequest } from '../../actions/api/requests';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import Tooltip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';
import classNames from 'classnames';

const QUARTZ_SCHEDULE = 'quartzSchedule';
const CRON_SCHEDULE = 'cronSchedule';

const FORM_ID = 'requestForm';

const REQUEST_TYPES = ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];

const FIELDS_BY_REQUEST_TYPE = {
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
    QUARTZ_SCHEDULE,
    CRON_SCHEDULE,
    'scheduleType',
    'numRetriesOnFailure',
    'killOldNonLongRunningTasksAfterMillis',
    'scheduledExpectedRuntimeMillis'
  ],
  ON_DEMAND: ['killOldNonLongRunningTasksAfterMillis'],
  RUN_ONCE: ['killOldNonLongRunningTasksAfterMillis']
};

class RequestForm extends React.Component {
  static propTypes = {
    racks:
  }

  componentDidMount() {
    this.props.clearForm(FORM_ID);
  }

  shouldComponentUpdate(nextProps) {
    return !_.isEqual(this.props, nextProps);
  }

  isEditing() {
    return this.props.request && this.props.request.request;
  }

  getValue(fieldId) {
    if (this.props.form && this.props.form[fieldId] !== undefined) {
      return this.props.form[fieldId];
    } else if (this.isEditing() && this.props.request.request[fieldId] !== undefined) {
      return this.props.request.request[fieldId];
    } else {
      return '';
    }
  }

  cantSubmit() {
    return this.props.saveApiCall.isFetching ||
      !this.getValue('id') ||
      !this.getValue('requestType') ||
      (this.getValue('requestType') === 'SCHEDULED' && !this.getValue(this.getScheduleType()));
  }

  submitForm(event) {
    event.preventDefault();
    const request = {};
    const copyOverField = (fieldId) => {
      if (this.getValue(fieldId) && fieldId != QUARTZ_SCHEDULE && fieldId != CRON_SCHEDULE && fieldId != 'scheduleType') {
        request[fieldId] = this.getValue(fieldId);
      }
    };

    FIELDS_BY_REQUEST_TYPE[this.getValue('requestType')].map(copyOverField);
    FIELDS_BY_REQUEST_TYPE.ALL.map(copyOverField);

    if (this.getValue('requestType') === 'SCHEDULED') {
      if (this.getScheduleType() === QUARTZ_SCHEDULE) {
        request[QUARTZ_SCHEDULE] = this.getValue(QUARTZ_SCHEDULE);
      } else {
        request.schedule = this.getValue(CRON_SCHEDULE);
      }
    }

    if (['ON_DEMAND', 'RUN_ONCE'].indexOf(this.getValue('requestType')) !== -1) {
      request.daemon = false;
    } else if (['SERVICE', 'WORKER'].indexOf(this.getValue('requestType')) !== -1) {
      request.daemon = true;
    }

    if (request.owners) {
      request.owners = request.owners.map(owner => owner.value);
    }
    if (request.rackAffinity) {
      request.rackAffinity = request.rackAffinity.map(rack => rack.value);
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
    this.props.update(FORM_ID, fieldId, newValue);
  }

  updateTypeButtonClick(event) {
    event.preventDefault();
    this.updateField('requestType', event.target.value);
  }

  getScheduleType() {
    if (this.isEditing() && !(this.props.form && this.props.form.scheduleType)) {
      if (this.props.request.request[QUARTZ_SCHEDULE]) {
        return QUARTZ_SCHEDULE;
      } else {
        return CRON_SCHEDULE;
      }
    } else {
      if (this.props.form && this.props.form.scheduleType) {
        return this.props.form.scheduleType;
      } else {
        return CRON_SCHEDULE;
      }
    }
  }

  renderRequestTypeSelectors() {
    const tooltip = (
      <Tooltip id="cannotChangeRequestTypeAfterCreation">Option cannot be altered after creation</Tooltip>
    );
    const selectors = REQUEST_TYPES.map((requestType, key) => {
      const selector = (
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
        return <OverlayTrigger placement="top" key={key} overlay={tooltip}>{selector}</OverlayTrigger>;
      } else {
        return selector;
      }
    });
    return <div className="btn-group">{selectors}</div>;
  }

  renderLoadBalanced() {
    const checkbox = (
      <label htmlFor="load-balanced" className={classNames({subtle: this.isEditing()})}>
        <CheckBox
          id = "load-balanced"
          onChange = {event => this.updateField('loadBalanced', !this.getValue('loadBalanced'))}
          checked = {this.getValue('loadBalanced')}
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
          overlay={<Tooltip id="cannotChangeLoadBalancedAfterCreation">Option cannot be altered after creation</Tooltip>}>
          {checkbox}
        </OverlayTrigger>
      );
    } else {
      field = checkbox;
    }
    return (
      <div className="form-group">
        {field}
      </div>
    );
  }

  renderRequestTypeSpecificFormFields() {
    const instances = (
      <div className="form-group">
        <label htmlFor="instances">Instances</label>
        <FormField
          id = "instances"
          prop = {{
            updateFn: event => this.updateField('instances', event.target.value),
            placeholder: '1',
            inputType: 'text',
            value: this.getValue('instances')
          }}
        />
      </div>
    );
    const rackSensitive = (
      <div className="form-group">
        <label htmlFor="rack-sensitive">
          <CheckBox
            id="rack-sensitive"
            onChange={event => this.updateField('rackSensitive', !this.getValue('rackSensitive'))}
            checked={this.getValue('rackSensitive')}
            noFormControlClass={true}
          />
          {" Rack Sensitive"}
        </label>
      </div>
    );
    const hideEvenNumberAcrossRacksHint = (
      <div className="form-group">
        <label htmlFor="hide-distribute-evenly-across-racks-hint">
          <CheckBox
            id="hide-distribute-evenly-across-racks-hint"
            onChange={event => this.updateField('hideEvenNumberAcrossRacksHint', !this.getValue('hideEvenNumberAcrossRacksHint'))}
            checked={this.getValue('hideEvenNumberAcrossRacksHint')}
            noFormControlClass={true}
          />
          {" Hide Distribute Evenly Across Racks Hint"}
        </label>
      </div>
    );
    const waitAtLeastMillisAfterTaskFinishesForReschedule = (
      <div className="form-group">
        <label htmlFor="waitAtLeast">Task rescheduling delay</label>
        <div className="input-group">
          <FormField
            id="waitAtLeast"
            prop={{
              updateFn: event => this.updateField('waitAtLeastMillisAfterTaskFinishesForReschedule', event.target.value),
              inputType: 'text',
              value: this.getValue('waitAtLeastMillisAfterTaskFinishesForReschedule')
            }}
          />
          <div className="input-group-addon">milliseconds</div>
        </div>
      </div>
    );
    const rackOptions = _.pluck(this.props.racks, 'id').map(id => ({value: id, label: id}));
    const rackAffinity = (
      <div className="form-group">
        <label htmlFor="rack-affinity">Rack Affinity <span className="form-label-tip">separate multiple racks with commas</span></label>
        <MultiSelect
          id="rack-affinity"
          onChange={ value => this.updateField('rackAffinity', value) }
          value={ this.getValue('rackAffinity') }
          options={rackOptions}
          splits={[',', ' ']}
        />
      </div>
    );
    const schedule = (
      <div className="form-group required">
        <label htmlFor="schedule">Schedule</label>
        <div className="row" id="schedule">
          <div className="col-sm-7">
            <FormField
              prop={{
                updateFn: event => this.updateField(this.getScheduleType(), event.target.value),
                placeholder: this.getScheduleType() === QUARTZ_SCHEDULE ? 'eg: 0 */5 * * * ?' : 'eg: */5 * * * *',
                inputType: 'text',
                value: this.getValue(this.getScheduleType())
              }}
            />
          </div>
          <div className="col-sm-5">
            <Select
              onChange={value => this.updateField('scheduleType', value)}
              options={[
                {
                  value: CRON_SCHEDULE,
                  label: 'Cron Schedule'
                },
                {
                  value: QUARTZ_SCHEDULE,
                  label: 'Quartz Schedule'
                }
              ]}
              clearable={false}
              value={ this.getScheduleType() }
            />
          </div>
        </div>
      </div>
    );
    const numRetriesOnFailure = (
      <div className="form-group">
        <label htmlFor="retries-on-failure">Number of retries on failure</label>
        <FormField
          id="retries-on-failure"
          prop={{
            updateFn: event => this.updateField('numRetriesOnFailure', event.target.value),
            inputType: 'text',
            value: this.getValue('numRetriesOnFailure')
          }}
        />
      </div>
    );
    const killOldNonLongRunningTasksAfterMillis = (
      <div className="form-group">
        <label htmlFor="killOldNRL">Kill cleaning task(s) after</label>
        <div className="input-group">
          <FormField
            id="killOldNRL"
            prop={{
              updateFn: event => this.updateField('killOldNonLongRunningTasksAfterMillis', event.target.value),
              inputType: 'text',
              value: this.getValue('killOldNonLongRunningTasksAfterMillis')
            }}
          />
          <div className="input-group-addon">milliseconds</div>
        </div>
      </div>
    );
    const scheduledExpectedRuntimeMillis = (
      <div className="form-group">
        <label htmlFor="expected-runtime">Maximum task duration</label>
        <div className="input-group">
          <FormField
            id="expected-runtime"
            prop={{
              updateFn: event => this.updateField('scheduledExpectedRuntimeMillis', event.target.value),
              inputType: 'text',
              value: this.getValue('scheduledExpectedRuntimeMillis')
            }}
          />
          <div className="input-group-addon">milliseconds</div>
        </div>
      </div>
    );
    return (
      <div>
        { this.shouldRenderField('instances') && instances }
        { this.shouldRenderField('rackSensitive') && rackSensitive }
        { this.shouldRenderField('hideEvenNumberAcrossRacksHint') && hideEvenNumberAcrossRacksHint }
        { this.shouldRenderField('loadBalanced') && this.renderLoadBalanced() }
        { this.shouldRenderField('waitAtLeastMillisAfterTaskFinishesForReschedule') && waitAtLeastMillisAfterTaskFinishesForReschedule }
        { this.shouldRenderField('rackAffinity') && rackAffinity }
        { (this.shouldRenderField(CRON_SCHEDULE) || this.shouldRenderField(QUARTZ_SCHEDULE)) && schedule }
        { this.shouldRenderField('numRetriesOnFailure') && numRetriesOnFailure }
        { this.shouldRenderField('killOldNonLongRunningTasksAfterMillis') && killOldNonLongRunningTasksAfterMillis }
        { this.shouldRenderField('scheduledExpectedRuntimeMillis') && scheduledExpectedRuntimeMillis }
      </div>
    );
  }

  render() {
    const requestId = this.isEditing() ? this.props.request.request.id : null;
    const header = (
      this.isEditing() ?
        <h3>
          Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a>
        </h3> :
        <h3>New Request</h3>
    );
    const id = (
      <div className="form-group required">
        <label htmlFor="id">ID</label>
        <FormField
          id="id"
          prop={{
            updateFn: event => {
              this.updateField('id', event.target.value);
            },
            placeholder: 'eg: my-awesome-request',
            inputType: 'text',
            value: this.getValue('id'),
            required: true
          }}
        />
      </div>
    );
    const owners = (
      <div className="form-group">
        <label htmlFor="owners">Owners <span className="form-label-tip">separate multiple owners with commas</span></label>
        <MultiSelect
          id="owners"
          onChange={ value => this.updateField('owners', value) }
          value={ this.getValue('owners') }
          splits={[',', ' ']}
        />
      </div>
    );
    const requestTypeSelectors = (
      <div className="form-group">
        <label>Type</label>
        <div id="type" className="btn-group">
          {this.renderRequestTypeSelectors()}
        </div>
      </div>
    );
    const onlyAffectsNewTasksWarning = (
      <div className="alert alert-info alert-slim" role="alert">
        <strong>Note:</strong> changes made below will only affect new tasks
      </div>
    );
    const slavePlacement = (
      <div className="form-group slavePlacement">
        <label htmlFor="slavePlacement" className="control-label">
          Slave Placement
        </label>
        <DropDown
          id="slavePlacement"
          prop={{
            updateFn: event => {
              this.updateField('slavePlacement', event.target.value);
            },
            forceChooseValue: true,
            choices: [
              {
                value: '',
                user: 'Default'
              },
              {
                value: 'SEPARATE',
                user: 'Separate'
              },
              {
                value: 'OPTIMISTIC',
                user: 'Optimistic'
              },
              {
                value: 'GREEDY',
                user: 'Greedy'
              }
            ],
            value: this.getValue('slavePlacement') || ''
          }}
        />
      </div>
    );
    const saveButton = (
      <div id="button-row">
        <span>
          <button type="submit" className="btn btn-success btn-lg" disabled={this.cantSubmit() && 'disabled'}>
            Save
          </button>
        </span>
      </div>
    );
    const errorMessage = (
      this.props.saveApiCall.error ?
        <p className="alert alert-danger">
          There was a problem saving your request: {this.props.saveApiCall.error.message}
        </p> :
        this.props.saveApiCall.data && this.props.saveApiCall.data.message ?
        <p className="alert alert-danger">
          There was a problem saving your request: {this.props.saveApiCall.data.message}
        </p> :
        null
    );
    return (
      <div className="row new-form">
        <div className="col-md-5 col-md-offset-3">
          { header }
          <form role="form" onSubmit={event => this.submitForm(event)}>
            { !this.isEditing() && id }
            { owners }
            { requestTypeSelectors }
            { this.isEditing() && onlyAffectsNewTasksWarning }
            { slavePlacement }
            { this.renderRequestTypeSpecificFormFields() }
            { saveButton }
            { errorMessage }
          </form>
        </div>
      </div>
    );
  }
}

function navigateToRequestIfSuccess(promiseResult) {
  if (promiseResult.type === 'SAVE_REQUEST_SUCCESS') {
    Backbone.history.navigate(`/request/${ promiseResult.data.request.id }`, {trigger: true});
  }
}

function mapStateToProps(state) {
  return {
    racks: state.api.racks.data,
    request: state.api.request ? state.api.request.data : null,
    form: state.ui.form[FORM_ID],
    saveApiCall: state.api.saveRequest
  };
}

function mapDispatchToProps(dispatch) {
  return {
    update(formId, fieldId, newValue) {
      dispatch(ModifyField(formId, fieldId, newValue));
    },
    clearForm(formId) {
      dispatch(ClearForm(formId));
    },
    save(requestBody) {
      dispatch(SaveRequest.trigger(requestBody)).then((response) => navigateToRequestIfSuccess(response));
    }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
