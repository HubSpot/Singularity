import React, {PropTypes} from 'react';
import Select from 'react-select';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import MultiSelect from '../common/formItems/MultiSelect';
import MultiInputFormGroup from '../common/formItems/formGroups/MultiInputFormGroup';
import SelectFormGroup from '../common/formItems/formGroups/SelectFormGroup';
import TextFormGroup from '../common/formItems/formGroups/TextFormGroup';
import CheckBoxFormGroup from '../common/formItems/formGroups/CheckBoxFormGroup';
import { modifyField, clearForm } from '../../actions/form';
import {SaveAction} from '../../actions/api/request';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
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
    clearForm: PropTypes.func.isRequired,
    update: PropTypes.func.isRequired,
    save: PropTypes.func.isRequired,
    racks: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.string.isRequired
    })).isRequired,
    request: PropTypes.shape({
      request: PropTypes.shape({
        id: PropTypes.string.isRequired,
        slavePlacement: PropTypes.oneOf(['', 'SEPARATE', 'SEPARATE_BY_REQUEST', 'GREEDY', 'OPTIMISTIC'])
      })
    }),
    saveApiCall: PropTypes.shape({
      isFetching: PropTypes.bool,
      error: PropTypes.shape({
        message: PropTypes.string
      }),
      data: PropTypes.shape({
        message: PropTypes.string
      })
    }).isRequired,
    form: PropTypes.shape({
      slavePlacement: PropTypes.oneOf(['', 'SEPARATE', 'SEPARATE_BY_REQUEST', 'GREEDY', 'OPTIMISTIC']),
      scheduleType: PropTypes.string
    })
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
    }
    if (this.isEditing() && this.props.request.request[fieldId] !== undefined) {
      return this.props.request.request[fieldId];
    }
    return '';
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
      if (this.getValue(fieldId) && fieldId !== QUARTZ_SCHEDULE && fieldId !== CRON_SCHEDULE && fieldId !== 'scheduleType') {
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


    if (request.rackAffinity) {
      request.rackAffinity = request.rackAffinity.map(rack => rack.value);
    }

    this.props.save(request);
    return null;
  }

  shouldRenderField(fieldId) {
    if (!this.getValue('requestType')) {
      return false;
    }
    if (FIELDS_BY_REQUEST_TYPE[this.getValue('requestType')].indexOf(fieldId) === -1) {
      return false;
    }
    return true;
  }

  getButtonsDisabled(type) {
    if (this.isEditing() && this.getValue('requestType') !== type) {
      return 'disabled';
    }
    return null;
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
      }
      return CRON_SCHEDULE;
    }
    if (this.props.form && this.props.form.scheduleType) {
      return this.props.form.scheduleType;
    }
    return CRON_SCHEDULE;
  }

  renderRequestTypeSelectors() {
    const tooltip = (
      <ToolTip id="cannotChangeRequestTypeAfterCreation">Option cannot be altered after creation</ToolTip>
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
      }
      return selector;
    });
    return <div className="btn-group">{selectors}</div>;
  }

  renderRequestTypeSpecificFormFields() {
    const instances = (
      <TextFormGroup
        id="instances"
        onChange={event => this.updateField('instances', event.target.value)}
        value={this.getValue('instances')}
        label="Instances"
        placeholder="1"
      />
    );
    const rackSensitive = (
      <CheckBoxFormGroup
        id="rack-sensitive"
        label="Rack Sensitive"
        checked={this.getValue('rackSensitive') || false}
        onChange={(newValue) => this.updateField('rackSensitive', newValue)}
      />
    );
    const hideEvenNumberAcrossRacksHint = (
      <CheckBoxFormGroup
        id="hide-distribute-evenly-across-racks-hint"
        label="Hide Distribute Evenly Across Racks Hint"
        checked={this.getValue('hideEvenNumberAcrossRacksHint') || false}
        onChange={(newValue) => this.updateField('hideEvenNumberAcrossRacksHint', newValue)}
      />
    );
    const loadBalanced = (
      <CheckBoxFormGroup
        id="load-balanced"
        label="Load balanced"
        checked={this.getValue('loadBalanced') || false}
        onChange={(newValue) => this.updateField('loadBalanced', newValue)}
        disabled={this.isEditing() && true}
        hasTooltip={this.isEditing() && true}
        tooltipText="Option cannot be altered after creation"
      />
    );
    const waitAtLeastMillisAfterTaskFinishesForReschedule = (
      <TextFormGroup
        id="waitAtLeast"
        onChange={event => this.updateField('waitAtLeastMillisAfterTaskFinishesForReschedule', event.target.value)}
        value={this.getValue('waitAtLeastMillisAfterTaskFinishesForReschedule')}
        label="Task rescheduling delay"
        inputGroupAddon="milliseconds"
      />
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
              onChange={value => this.updateField('scheduleType', value.value)}
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
      <TextFormGroup
        id="retries-on-failure"
        onChange={event => this.updateField('numRetriesOnFailure', event.target.value)}
        value={this.getValue('numRetriesOnFailure')}
        label="Number of retries on failure"
      />
    );
    const killOldNonLongRunningTasksAfterMillis = (
      <TextFormGroup
        id="killOldNRL"
        onChange={event => this.updateField('killOldNonLongRunningTasksAfterMillis', event.target.value)}
        value={this.getValue('killOldNonLongRunningTasksAfterMillis')}
        label="Kill cleaning task(s) after"
        inputGroupAddon="milliseconds"
      />
    );
    const scheduledExpectedRuntimeMillis = (
      <TextFormGroup
        id="expected-runtime"
        onChange={event => this.updateField('scheduledExpectedRuntimeMillis', event.target.value)}
        value={this.getValue('scheduledExpectedRuntimeMillis')}
        label="Maximum task duration"
        inputGroupAddon="milliseconds"
      />
    );
    return (
      <div>
        { this.shouldRenderField('instances') && instances }
        { this.shouldRenderField('rackSensitive') && rackSensitive }
        { this.shouldRenderField('hideEvenNumberAcrossRacksHint') && hideEvenNumberAcrossRacksHint }
        { this.shouldRenderField('loadBalanced') && loadBalanced }
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
          Editing <a href={`${config.appRoot}/request/${requestId}`}>{requestId}</a>
        </h3> :
        <h3>New Request</h3>
    );
    const id = (
      <TextFormGroup
        id="id"
        onChange={event => this.updateField('id', event.target.value)}
        value={this.getValue('id')}
        label="ID"
        required={true}
        placeholder="eg: my-awesome-request"
      />
    );
    const owners = (
      <MultiInputFormGroup
        id="owners"
        value={this.getValue('owners') || []}
        onChange={(newValue) => this.updateField('owners', newValue)}
        label="Owners"
      />
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
      <SelectFormGroup
        id="slave-placement"
        label="Slave Placement"
        value={this.getValue('slavePlacement') || ''}
        defaultValue=""
        onChange={newValue => this.updateField('slavePlacement', newValue.value)}
        options={[
          { label: 'Default', value: '' },
          { label: 'Separate', value: 'SEPARATE' },
          { label: 'Optimistic', value: 'OPTIMISTIC' },
          { label: 'Greedy', value: 'GREEDY' },
          { label: 'Separate by request', value: 'SEPARATE_BY_REQUEST'}
        ]}
      />
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
      this.props.saveApiCall.error &&
      <p className="alert alert-danger">
        There was a problem saving your request: {this.props.saveApiCall.error.message}
      </p> ||
      this.props.saveApiCall.data && this.props.saveApiCall.data.message &&
      <p className="alert alert-danger">
        There was a problem saving your request: {this.props.saveApiCall.data.message}
      </p>
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
    form: state.form[FORM_ID],
    saveApiCall: state.api.saveRequest
  };
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
      dispatch(SaveAction.trigger(requestBody)).then((response) => navigateToRequestIfSuccess(response));
    }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
