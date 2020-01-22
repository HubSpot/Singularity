import React, {PropTypes} from 'react';
import { connect } from 'react-redux';

import { Link } from 'react-router';
import { withRouter } from 'react-router';

import rootComponent from '../../rootComponent';
import MultiSelect from '../common/formItems/MultiSelect';
import MultiInputFormGroup from '../common/formItems/formGroups/MultiInputFormGroup';
import SelectFormGroup from '../common/formItems/formGroups/SelectFormGroup';
import TextFormGroup from '../common/formItems/formGroups/TextFormGroup';
import CheckboxFormGroup from '../common/formItems/formGroups/CheckboxFormGroup';
import MapInputFormGroup from '../common/formItems/formGroups/MapInputFormGroup';
import { ModifyField, ClearForm } from '../../actions/ui/form';
import { SaveRequest, FetchRequest } from '../../actions/api/requests';
import { OverlayTrigger, Tooltip} from 'react-bootstrap/lib';
import { Form, Row, Col, Glyphicon } from 'react-bootstrap';
import Utils from '../../utils';
import timeZones from '../../timeZones';
import classNames from 'classnames';
import {FIELDS_BY_REQUEST_TYPE, INDEXED_FIELDS} from './fields';
import { FetchRacks } from '../../actions/api/racks';
import { refresh } from '../../actions/ui/requestForm';

const QUARTZ_SCHEDULE = 'quartzSchedule';
const CRON_SCHEDULE = 'cronSchedule';

const FORM_ID = 'requestForm';

const REQUEST_ID_REGEX = /[a-zA-Z0-9._-]*/;

const timeZoneOptions = timeZones.map(zone => ({label: zone, value: zone}));

const RequestForm = (props) => {
  const isEditing = props.request && props.request.request;

  let scheduleType;
  if (isEditing && !(props.form && props.form.scheduleType)) {
    if (props.request.request[QUARTZ_SCHEDULE]) {
      scheduleType = QUARTZ_SCHEDULE;
    } else {
      scheduleType = CRON_SCHEDULE;
    }
  } else if (props.form && props.form.scheduleType) {
    scheduleType = props.form.scheduleType;
  } else {
    scheduleType = CRON_SCHEDULE;
  }

  const getValue = (fieldId) => {
    if (props.form && props.form[fieldId] !== undefined) {
      return props.form[fieldId];
    }
    if (isEditing && props.request.request[fieldId] !== undefined) {
      if (_.isObject(INDEXED_FIELDS[fieldId].type)) {
        if (INDEXED_FIELDS[fieldId].type.typeName === 'map') {
          return Utils.convertMapFromObjectToArray(props.request.request[fieldId]);
        }
      }
      return props.request.request[fieldId];
    }
    return '';
  };

  const validateType = (type, value) => {
    if (!value || _.isEmpty(value)) {
      return true;
    }
    if (_.isObject(type)) {
      if (type.typeName === 'array') {
        if (!_.isArray(value)) return false;
        for (const subValue of value) {
          if (!validateType(type.arrayType, subValue)) return false;
        }
        return true;
      }
      if (type.typeName === 'map') {
        if (!_.isArray(value)) return false;
        for (const pair of value) {
          if (!_.isObject(pair)) return false;
          if (!pair.key) return false;
          if (!validateType(type.mapFrom, pair.key)) return false;
          if (!validateType(type.mapTo, pair.value)) return false;
        }
        return true;
      }
      if (type.typeName === 'enum') {
        return Utils.isIn(value, type.enumType);
      }
    }
    if (type === 'number') {
      const numericalValue = parseInt(value, 10);
      if (numericalValue !== 0 && !numericalValue) {
        return false;
      }
    }
    if (type === 'request-id') {
      if (value.match(REQUEST_ID_REGEX)[0] !== value) {
        return false;
      }
    }
    if (type === 'map') {
      for (const element of value) {
        if (element.split('=').length !== 2) {
          return false;
        }
      }
    }
    return true;
  };

  const validateField = (fieldId) => {
    const value = getValue(fieldId);
    const {required, type} = INDEXED_FIELDS[fieldId];
    if (required && (_.isEmpty(value))) {
      return false;
    }
    return validateType(type, value);
  };

  const feedback = (fieldId) => {
    const value = getValue(fieldId);
    const {required} = INDEXED_FIELDS[fieldId];
    if (required && (_.isEmpty(value))) {
      return 'ERROR';
    }
    if (_.isEmpty(value)) {
      return null;
    }
    if (validateField(fieldId)) {
      return 'SUCCESS';
    }
    return 'ERROR';
  };

  const cantSubmit = () => {
    if (props.saveApiCall.isFetching) {
      return true;
    }
    for (const field of FIELDS_BY_REQUEST_TYPE.ALL) {
      if (!validateField(field.id)) {
        return true;
      }
    }
    const requestTypeSpecificFields = FIELDS_BY_REQUEST_TYPE[getValue('requestType')];
    if (_.isEmpty(requestTypeSpecificFields)) {
      return true;
    }
    for (const field of requestTypeSpecificFields) {
      if (field.id === CRON_SCHEDULE && scheduleType !== CRON_SCHEDULE) {
        continue;
      }
      if (field.id === QUARTZ_SCHEDULE && scheduleType !== QUARTZ_SCHEDULE) {
        continue;
      }
      if (!validateField(field.id)) {
        return true;
      }
    }
    return false;
  };

  const submitForm = (event) => {
    event.preventDefault();
    const request = {};
    const copyOverField = (field) => {
      const fieldId = field.id;
      if (getValue(fieldId) && fieldId !== QUARTZ_SCHEDULE && fieldId !== CRON_SCHEDULE && fieldId !== 'scheduleType') {
        if (_.isObject(field.type) && field.type.typeName === 'map') {
          request[fieldId] = Utils.convertMapFromArrayToObject(getValue(fieldId));
        } else {
          request[fieldId] = getValue(fieldId);
        }
      }
    };

    FIELDS_BY_REQUEST_TYPE[getValue('requestType')].map(copyOverField);
    FIELDS_BY_REQUEST_TYPE.ALL.map(copyOverField);

    if (getValue('requestType') === 'SCHEDULED') {
      if (scheduleType === QUARTZ_SCHEDULE) {
        request[QUARTZ_SCHEDULE] = getValue(QUARTZ_SCHEDULE);
      } else {
        request.schedule = getValue(CRON_SCHEDULE);
      }
    }

    if (['ON_DEMAND', 'RUN_ONCE'].indexOf(getValue('requestType')) !== -1) {
      request.daemon = false;
    } else if (['SERVICE', 'WORKER'].indexOf(getValue('requestType')) !== -1) {
      request.daemon = true;
    }

    props.save(request);
    return null;
  };

  const shouldRenderField = (fieldId) => {
    if (_.pluck(FIELDS_BY_REQUEST_TYPE.ALL, 'id').indexOf(fieldId) !== -1) {
      return true;
    }
    if (!getValue('requestType')) {
      return false;
    }
    if (_.pluck(FIELDS_BY_REQUEST_TYPE[getValue('requestType')], 'id').indexOf(fieldId) === -1) {
      return false;
    }
    return true;
  };

  const getButtonsDisabled = (type) => {
    if (isEditing && getValue('requestType') !== type) {
      return 'disabled';
    }
    return null;
  };

  const updateField = (fieldId, newValue) => props.update(fieldId, newValue);

  const updateTypeButtonClick = (event) => {
    event.preventDefault();
    updateField('requestType', event.target.value);
  };

  const renderRequestTypeSelectors = () => {
    const tooltip = (
      <Tooltip id="cannotChangeRequestTypeAfterCreation">Option cannot be altered after creation</Tooltip>
    );
    const selectors = Utils.enums.SingularityRequestTypes.map((requestType, key) => {
      const selector = (
        <button
          key={key}
          value={requestType}
          className={classNames('btn', 'btn-default', {active: getValue('requestType') === requestType})}
          onClick={event => updateTypeButtonClick(event)}
          disabled={getButtonsDisabled(requestType)}
        >
          {Utils.humanizeText(requestType)}
        </button>
      );
      if (isEditing && requestType === getValue('requestType')) {
        return <OverlayTrigger placement="top" key={key} overlay={tooltip}>{selector}</OverlayTrigger>;
      }
      return selector;
    });
    return <div className="btn-group">{selectors}</div>;
  };

  const requestId = isEditing ? props.request.request.id : null;
  const header = (
    isEditing ?
      <h3>
        Editing <Link to={`request/${requestId}`}>{requestId}</Link>
      </h3> :
      <h3>New Request</h3>
  );
  const id = (
    <TextFormGroup
      id="id"
      onChange={event => updateField('id', event.target.value)}
      value={getValue('id')}
      label="ID"
      required={INDEXED_FIELDS.id.required}
      placeholder="eg: my-awesome-request"
      feedback={feedback('id')}
    />
  );
  const owners = (
    <MultiInputFormGroup
      id="owners"
      value={getValue('owners') || []}
      onChange={(newValue) => updateField('owners', newValue)}
      label="Owners"
      required={INDEXED_FIELDS.owners.required}
      errorIndices={INDEXED_FIELDS.owners.required && _.isEmpty(getValue('owners')) && [0] || []}
      couldHaveFeedback={true}
    />
  );
  const requestTypeSelectors = (
    <div className="form-group">
      <label>Type</label>
      <div id="type" className="btn-group">
        {renderRequestTypeSelectors()}
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
      value={getValue('slavePlacement') || ''}
      defaultValue=""
      required={INDEXED_FIELDS.slavePlacement.required}
      onChange={newValue => updateField('slavePlacement', newValue.value)}
      options={[
        { label: 'Default', value: '' },
        { label: 'Separate', value: 'SEPARATE' },
        { label: 'Optimistic', value: 'OPTIMISTIC' },
        { label: 'Greedy', value: 'GREEDY' },
        { label: 'Separate by request', value: 'SEPARATE_BY_REQUEST'},
        { label: 'Spread all workers', value: 'SPREAD_ALL_SLAVES'}
      ]}
    />
  );

  const instances = (
    <TextFormGroup
      id="instances"
      onChange={event => updateField('instances', event.target.value)}
      value={getValue('instances')}
      label="Instances"
      placeholder="1"
      feedback={feedback('instances')}
      required={INDEXED_FIELDS.instances.required}
    />
  );

  const rackSensitive = (
    <CheckboxFormGroup
      id="rack-sensitive"
      label="Rack sensitive"
      checked={getValue('rackSensitive') || false}
      onChange={(newValue) => updateField('rackSensitive', newValue)}
    />
  );

  const hideEvenNumberAcrossRacksHint = (
    <CheckboxFormGroup
      id="hide-distribute-evenly-across-racks-hint"
      label="Hide distribute evenly across racks hint"
      checked={getValue('hideEvenNumberAcrossRacksHint') || false}
      onChange={(newValue) => updateField('hideEvenNumberAcrossRacksHint', newValue)}
    />
  );

  const loadBalanced = (
    <CheckboxFormGroup
      id="load-balanced"
      label="Load balanced"
      checked={getValue('loadBalanced') || false}
      onChange={(newValue) => updateField('loadBalanced', newValue)}
      disabled={isEditing && true}
      hasTooltip={isEditing && true}
      tooltipText="Option cannot be altered after creation"
    />
  );

  const allowBounceToSameHost = (
    <CheckboxFormGroup
      id="allow-bounce-to-same-host"
      label="Allow Bounce To Same Host"
      checked={getValue('allowBounceToSameHost') || false}
      onChange={(newValue) => updateField('allowBounceToSameHost', newValue)}
    />
  );

  const waitAtLeastMillisAfterTaskFinishesForReschedule = (
    <TextFormGroup
      id="waitAtLeast"
      onChange={event => updateField('waitAtLeastMillisAfterTaskFinishesForReschedule', event.target.value)}
      value={getValue('waitAtLeastMillisAfterTaskFinishesForReschedule')}
      label="Task rescheduling delay"
      inputGroupAddon="milliseconds"
      required={INDEXED_FIELDS.waitAtLeastMillisAfterTaskFinishesForReschedule.required}
      feedback={feedback('waitAtLeastMillisAfterTaskFinishesForReschedule')}
    />
  );

  const rackOptions = _.pluck(props.racks, 'id').map(rackId => ({value: rackId, label: rackId}));
  const rackAffinity = (
    <div className="form-group">
      <label htmlFor="rack-affinity">Rack affinity <span className="form-label-tip">choose any subset</span></label>
      <MultiSelect
        id="rack-affinity"
        onChange={value => updateField('rackAffinity', value)}
        value={getValue('rackAffinity') || []}
        isValueString={true}
        options={rackOptions}
        splits={[',', ' ']}
      />
    </div>
  );

  const scheduleTypeField = (
    <SelectFormGroup
      id="schedule-type"
      label="Schedule type"
      value={scheduleType || ''}
      defaultValue={CRON_SCHEDULE}
      required={INDEXED_FIELDS.scheduleType.required}
      onChange={newValue => updateField('scheduleType', newValue.value)}
      options={[
        {value: CRON_SCHEDULE, label: 'Cron Schedule'},
        {value: QUARTZ_SCHEDULE, label: 'Quartz Schedule'}
      ]}
    />
  );

  const scheduleTimeZone = (
    <SelectFormGroup
      id="schedule-timezone"
      onChange={newValue => updateField('scheduleTimeZone', newValue ? newValue.value : null)}
      value={getValue('scheduleTimeZone') || ''}
      label="Schedule timezone"
      required={INDEXED_FIELDS.scheduleTimeZone.required}
      clearable={true}
      options={timeZoneOptions}
    />
  );

  const schedule = (
    <TextFormGroup
      id="schedule"
      onChange={event => updateField(scheduleType, event.target.value)}
      value={getValue(scheduleType)}
      label="Schedule"
      required={INDEXED_FIELDS[scheduleType].required}
      placeholder={scheduleType === QUARTZ_SCHEDULE ? 'eg: 0 */5 * * * ?' : 'eg: */5 * * * *'}
      feedback={feedback(scheduleType)}
    />
  );

  const numRetriesOnFailure = (
    <TextFormGroup
      id="retries-on-failure"
      onChange={event => updateField('numRetriesOnFailure', event.target.value)}
      value={getValue('numRetriesOnFailure')}
      label="Number of retries on failure"
      required={INDEXED_FIELDS.numRetriesOnFailure.required}
      feedback={feedback('numRetriesOnFailure')}
    />
  );

  const killOldNonLongRunningTasksAfterMillis = (
    <TextFormGroup
      id="killOldNRL"
      onChange={event => updateField('killOldNonLongRunningTasksAfterMillis', event.target.value)}
      value={getValue('killOldNonLongRunningTasksAfterMillis')}
      label="Kill cleaning task(s) after"
      inputGroupAddon="milliseconds"
      required={INDEXED_FIELDS.killOldNonLongRunningTasksAfterMillis.required}
      feedback={feedback('killOldNonLongRunningTasksAfterMillis')}
    />
  );

  const scheduledExpectedRuntimeMillis = (
    <TextFormGroup
      id="expected-runtime"
      onChange={event => updateField('scheduledExpectedRuntimeMillis', event.target.value)}
      value={getValue('scheduledExpectedRuntimeMillis')}
      label="Expected Task Duration (used for overdue notifications)"
      inputGroupAddon="milliseconds"
      required={INDEXED_FIELDS.scheduledExpectedRuntimeMillis.required}
      feedback={feedback('scheduledExpectedRuntimeMillis')}
    />
  );

  const taskExecutionTimeLimitMillis = (
    <TextFormGroup
      id="expected-runtime"
      onChange={event => updateField('taskExecutionTimeLimitMillis', event.target.value)}
      value={getValue('taskExecutionTimeLimitMillis')}
      label="Maximum task duration (task will be killed after this time)"
      inputGroupAddon="milliseconds"
      required={INDEXED_FIELDS.taskExecutionTimeLimitMillis.required}
      feedback={feedback('taskExecutionTimeLimitMillis')}
    />
  );

  const showAdvanced = getValue('showAdvanced');
  const advancedSelector = (
    <a onClick={() => updateField('showAdvanced', !showAdvanced)}>
      Advanced <Glyphicon glyph={showAdvanced ? 'chevron-down' : 'chevron-right'} />
    </a>
  );

  const requiredSlaveAttributes = (
    <MapInputFormGroup
      id="required-slave-attributes"
      onChange={newValue => updateField('requiredSlaveAttributes', newValue)}
      value={getValue('requiredSlaveAttributes') || []}
      label="Required slave attributes"
      required={INDEXED_FIELDS.requiredSlaveAttributes.required}
      doFeedback={true}
      keyHeader="Attribute"
      valueHeader="Value"
    />
  );

  const allowedSlaveAttributes = (
    <MapInputFormGroup
      id="allowed-slave-attributes"
      onChange={newValue => updateField('allowedSlaveAttributes', newValue)}
      value={getValue('allowedSlaveAttributes') || []}
      label="Allowed slave attributes"
      required={INDEXED_FIELDS.allowedSlaveAttributes.required}
      doFeedback={true}
      keyHeader="Attribute"
      valueHeader="Value"
    />
  );

  const group = (
    <TextFormGroup
      id="group"
      onChange={event => updateField('group', event.target.value)}
      value={getValue('group')}
      label="Group"
      required={INDEXED_FIELDS.group.required}
      feedback={feedback('group')}
    />
  );

  const readOnlyGroups = (
    <MultiInputFormGroup
      id="read-only-groups"
      value={getValue('readOnlyGroups') || []}
      onChange={(newValue) => updateField('readOnlyGroups', newValue)}
      label="Read-only groups"
      required={INDEXED_FIELDS.readOnlyGroups.required}
      errorIndices={INDEXED_FIELDS.readOnlyGroups.required && _.isEmpty(getValue('readOnlyGroups')) && [0] || []}
      couldHaveFeedback={true}
    />
  );

  const readWriteGroups = (
    <MultiInputFormGroup
      id="read-write-groups"
      value={getValue('readWriteGroups') || []}
      onChange={(newValue) => updateField('readWriteGroups', newValue)}
      label="Read-write groups"
      required={INDEXED_FIELDS.readWriteGroups.required}
      errorIndices={INDEXED_FIELDS.readWriteGroups.required && _.isEmpty(getValue('readWriteGroups')) && [0] || []}
      couldHaveFeedback={true}
    />
  );

  const maxTasksPerOffer = (
    <TextFormGroup
      id="max-per-offer"
      onChange={event => updateField('maxTasksPerOffer', event.target.value)}
      value={getValue('maxTasksPerOffer')}
      label="Schedule at most this many tasks using a single offer form a single slave"
      required={INDEXED_FIELDS.maxTasksPerOffer.required}
      feedback={feedback('maxTasksPerOffer')}
    />
  );

  const taskLogErrorRegex = (
    <TextFormGroup
      id="task-log-error-regex"
      onChange={event => updateField('taskLogErrorRegex', event.target.value)}
      value={getValue('taskLogErrorRegex')}
      label="Regex that matches errors in task logs to send in emails for this request"
      required={INDEXED_FIELDS.taskLogErrorRegex.required}
      feedback={feedback('taskLogErrorRegex')}
    />
  );

  const taskLogErrorRegexCaseSensitive = (
    <CheckboxFormGroup
      id="task-log-error-regex-case-sensitive"
      label="The above task log error regex is case-sensitive"
      checked={getValue('taskLogErrorRegexCaseSensitive') || false}
      onChange={(newValue) => updateField('taskLogErrorRegexCaseSensitive', newValue)}
    />
  );

  const renderEmailTypeSelector = (currentValue, onChange) => (
    <SelectFormGroup
      id="email-type-selector"
      value={currentValue || ''}
      onChange={newValue => onChange(newValue && newValue.value || null)}
      options={Utils.enums.SingularityEmailType.map(emailType => ({label: Utils.humanizeText(emailType), value: emailType}))}
      clearable={true}
      selectorsOnly={true}
    />
  );

  const renderEmailDestinationSelector = (currentValue, onChange) => (
    <MultiSelect
      id="email-destination-selector"
      value={currentValue || []}
      onChange={onChange}
      options={Utils.enums.SingularityEmailDestination.map(emailDestination => ({label: Utils.humanizeText(emailDestination), value: emailDestination}))}
      isValueString={true}
      clearable={true}
      splits={[',', ' ']}
    />
  );

  const emailConfigurationOverrides = (
    <MapInputFormGroup
      id="email-configuration-overrides"
      onChange={newValue => updateField('emailConfigurationOverrides', newValue)}
      value={getValue('emailConfigurationOverrides') || []}
      label="Email configuration overrides"
      required={INDEXED_FIELDS.requiredSlaveAttributes.required}
      renderKeyField={renderEmailTypeSelector}
      renderValueField={renderEmailDestinationSelector}
      valueDefault={[]}
      doFeedback={true}
      keyHeader="Email type"
      valueHeader="Email destination(s)"
    />
  );

  const bounceAfterScale = (
    <CheckboxFormGroup
      id="bounce-after-scale"
      label="Bounce each time this request is scaled"
      checked={getValue('bounceAfterScale') || false}
      onChange={(newValue) => updateField('bounceAfterScale', newValue)}
    />
  );

  const skipHealthchecks = (
    <CheckboxFormGroup
      id="skip-healthchecks"
      label="Skip healthchecks"
      checked={getValue('skipHealthchecks') || false}
      onChange={(newValue) => updateField('skipHealthchecks', newValue)}
    />
  );

  const saveButton = (
    <div id="button-row">
      <span>
        <button type="submit" className="btn btn-success btn-lg" disabled={cantSubmit() && 'disabled'}>
          Save
        </button>
      </span>
    </div>
  );
  const errorMessage = (
    props.saveApiCall.error && props.saveApiCall.error.message &&
    <p className="alert alert-danger">
      There was a problem saving your request: {props.saveApiCall.error.message}
    </p> ||
    props.saveApiCall.error &&
    <p className="alert alert-danger">
      There was a problem saving your request: {props.saveApiCall.error}
    </p> ||
    props.saveApiCall.data && props.saveApiCall.data.message &&
    <p className="alert alert-danger">
      There was a problem saving your request: {props.saveApiCall.data.message}
    </p>
  );
  return (
    <Row className="new-form">
      <Col md={5} mdOffset={3}>
        { header }
        <Form onSubmit={event => submitForm(event)}>
          { !isEditing && id }
          { owners }
          { requestTypeSelectors }
          { isEditing && onlyAffectsNewTasksWarning }
          { slavePlacement }
          { shouldRenderField('instances') && instances }
          { shouldRenderField('rackSensitive') && rackSensitive }
          { shouldRenderField('hideEvenNumberAcrossRacksHint') && hideEvenNumberAcrossRacksHint }
          { shouldRenderField('loadBalanced') && loadBalanced }
          { shouldRenderField('allowBounceToSameHost') && allowBounceToSameHost }
          { shouldRenderField('waitAtLeastMillisAfterTaskFinishesForReschedule') && waitAtLeastMillisAfterTaskFinishesForReschedule }
          { shouldRenderField('rackAffinity') && rackAffinity }
          { shouldRenderField('scheduleType') && scheduleTypeField }
          { shouldRenderField('scheduleTimeZone') && scheduleTimeZone }
          { (shouldRenderField(CRON_SCHEDULE) || shouldRenderField(QUARTZ_SCHEDULE)) && schedule }
          { shouldRenderField('numRetriesOnFailure') && numRetriesOnFailure }
          { shouldRenderField('killOldNonLongRunningTasksAfterMillis') && killOldNonLongRunningTasksAfterMillis }
          { shouldRenderField('scheduledExpectedRuntimeMillis') && scheduledExpectedRuntimeMillis }
          { shouldRenderField('taskExecutionTimeLimitMillis') && taskExecutionTimeLimitMillis }
          <div>
            <hr />
            {advancedSelector}
            {showAdvanced && (
              <div className="well">
                <h4>Advanced Request Options</h4>
                <fieldset>
                  { shouldRenderField('requiredSlaveAttributes') && requiredSlaveAttributes }
                  { shouldRenderField('allowedSlaveAttributes') && allowedSlaveAttributes }
                  { shouldRenderField('group') && group }
                  { shouldRenderField('readOnlyGroups') && readOnlyGroups }
                  { shouldRenderField('readWriteGroups') && readWriteGroups }
                  { shouldRenderField('maxTasksPerOffer') && maxTasksPerOffer }
                  { shouldRenderField('taskLogErrorRegex') && taskLogErrorRegex }
                  { shouldRenderField('taskLogErrorRegexCaseSensitive') && taskLogErrorRegexCaseSensitive }
                  { shouldRenderField('emailConfigurationOverrides') && emailConfigurationOverrides }
                  { shouldRenderField('skipHealthchecks') && skipHealthchecks }
                  { shouldRenderField('bounceAfterScale') && bounceAfterScale }
                </fieldset>
              </div>
            )}
          </div>
          { saveButton }
          { errorMessage }
        </Form>
      </Col>
    </Row>
  );
};

RequestForm.propTypes = {
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
    error: PropTypes.oneOfType([
      PropTypes.shape({
        message: PropTypes.string
      }),
      PropTypes.string
    ]),
    data: PropTypes.shape({
      message: PropTypes.string
    })
  }).isRequired,
  form: PropTypes.shape({
    slavePlacement: PropTypes.oneOf(['', 'SEPARATE', 'SEPARATE_BY_REQUEST', 'GREEDY', 'OPTIMISTIC']),
    scheduleType: PropTypes.string
  }),
  router: PropTypes.object.isRequired
};

function mapStateToProps(state, ownProps) {
  const request = ownProps.params.requestId && state.api.request[ownProps.params.requestId];
  return {
    notFound: request && request.statusCode === 404,
    pathname: ownProps.location.pathname,
    racks: state.api.racks.data,
    request: request && request.data,
    form: state.ui.form[FORM_ID],
    saveApiCall: state.api.saveRequest
  };
}

function mapDispatchToProps(dispatch, ownProps) {
  return {
    update(fieldId, newValue) {
      dispatch(ModifyField(FORM_ID, fieldId, newValue));
    },
    clearForm(formId) {
      dispatch(ClearForm(formId));
    },
    save(requestBody) {
      dispatch(SaveRequest.trigger(requestBody)).then((response) => {
        if (response.type === 'SAVE_REQUEST_SUCCESS') {
          ownProps.router.push(`request/${response.data.request.id}`);
        }
      });
    },
    fetchRequest(requestId) {
      dispatch(FetchRequest.trigger(requestId, true));
    },
    fetchRacks() {
      dispatch(FetchRacks.trigger());
    },
    clearRequestData() {
      dispatch(FetchRequest.clearData());
    },
    clearSaveRequestData() {
      dispatch(SaveRequest.clearData());
    }
  };
}

export default withRouter(connect(
  mapStateToProps,
  mapDispatchToProps
)(rootComponent(RequestForm, (props) => refresh(props.params.requestId, FORM_ID), false)));
