import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import { modifyField, clearForm } from '../../actions/form';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';
import classNames from 'classnames';

let FORM_ID = 'requestForm';

let REQUEST_TYPES = ['SERVICE', 'WORKER', 'SCHEDULED', 'ON_DEMAND', 'RUN_ONCE'];

class RequestForm extends React.Component {

    componentDidMount() {
        this.props.clearForm(FORM_ID);
    }

    cantSubmit() {
        return false;
    }

    submitForm(props, event) {
        event.preventDefault();
        return null;
    }

    getButtonsDisabled(type) {
        if (this.hasOldValues() && this.getValue('requestType') !== type) {
            return 'disabled';
        } else {
            return null;
        }
    }

    updateTypeButtonClick(props, event) {
        event.preventDefault();
        props.update(FORM_ID, 'requestType', event.target.value);
    }

    hasOldValues() {
        return this.props.edit && this.props.request && this.props.request.request;
    }

    getValue(fieldId) {
        if (this.props.form && this.props.form[fieldId]) {
            return this.props.form[fieldId];
        } else if (this.hasOldValues() && this.props.request.request[fieldId]) {
            return this.props.request.request[fieldId];
        }
    }

    getScheduleType() {
        if (this.hasOldValues() && !(this.props.form && this.props.form.scheduleType)) {
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

    getActive(type) {
        if (this.getValue('requestType') === type) {
            return 'active';
        }
    }

    renderBasicFormField(htmlId, fieldId, labelText, {placeholder, inputGroupAddon, inputGroupAddonExtraClasses, required, generateSelectBox, selectBoxOptions} = {}) {
        return (
            <div className={classNames('form-group', {required: required})}>
                <label htmlFor={htmlId}>{labelText}</label>
                <div className={inputGroupAddon ? "input-group" : null}>
                    <FormField
                        id = {htmlId}
                        prop = {{
                            updateFn: event => {
                                this.props.update(FORM_ID, fieldId, event.target.value);
                            },
                            placeholder: placeholder,
                            inputType: 'text',
                            value: this.getValue(fieldId),
                            generateSelectBox: generateSelectBox,
                            selectBoxOptions: selectBoxOptions
                        }}
                    />
                    {inputGroupAddon ? <div className={classNames("input-group-addon", inputGroupAddonExtraClasses)}>{inputGroupAddon}</div> : null}
                </div>
            </div>
        );
    }

    renderCheckbox(htmlId, fieldId, labelText) {
        return (
            <div className={classNames('form-group', htmlId)}>
                <label htmlFor={htmlId} className="control-label">
                    {labelText}
                    <FormField
                        id = {htmlId}
                        prop = {{
                            updateFn: event => {
                                this.props.update(FORM_ID, fieldId, !this.getValue(fieldId));
                            },
                            inputType: 'checkBox',
                            checked: this.getValue(fieldId)
                        }}
                    />
                </label>
            </div>
        );
    }

    renderDropdown(htmlId, fieldId, choices, {defaultChoice, generateSelectBox, selectBoxOptions, labelText} = {}) {
        let value = this.getValue(fieldId);
        let dropDown = (
            <DropDown
                id = {htmlId}
                prop = {{
                    updateFn: event => {
                        this.props.update(FORM_ID, fieldId, event.target.value);
                    },
                    forceChooseValue: true,
                    choices: choices,
                    value: value ? value : defaultChoice,
                    generateSelectBox: generateSelectBox,
                    selectBoxOptions: selectBoxOptions
                }}
            />
        )
        if (labelText) {
            return (
                <div className={classNames('form-group', htmlId)}>
                    <label htmlFor={htmlId} className="control-label">
                        {labelText}
                    </label>
                    {dropDown}
                </div>
            );
        } else {
            return dropDown
        }
    }

    renderHeader() {
        if (this.hasOldValues()) {
            return <h3>Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a></h3>
        } else {
            return <h3>New Request</h3>
        }
    }

    renderId() {
        return this.renderBasicFormField(
            "id",
            "requestId",
            "ID",
            {
                placeholder: "eg: my-awesome-request",
                required: true
            }
        );
    }

    renderOwners() {
        return this.renderBasicFormField(
            'owners',
            'owners',
            'Owners',
            {
                generateSelectBox: true,
                selectBoxOptions: {
                    tags: [],
                    containerCssClass: 'select-owners hide-select2-spinner',
                    dropdownCssClass: 'hidden',
                    selectOnBlur: true,
                    tokenSeparators: [',',' ']
                }
            }
        );
    }

    renderRequestTypeSelectors() {
        let selectors = [];
        let tooltip = (
            <ToolTip id="cannotChangeAfterCreation">Option cannot be altered after creation</ToolTip>
        );
        REQUEST_TYPES.map((requestType, key) => {
            let selector = (
                <button
                    key={key}
                    value={requestType}
                    className={`btn btn-default ${this.getActive(requestType)}`}
                    onClick={event => this.updateTypeButtonClick(this.props, event)}
                    disabled={this.getButtonsDisabled(requestType)}
                >
                    {Utils.humanizeText(requestType)}
                </button>
            );
            if (this.hasOldValues() && requestType === this.getValue('requestType')) {
                selectors.push (<OverlayTrigger placement="top" key={key} overlay={tooltip}>{selector}</OverlayTrigger>);
            } else {
                selectors.push (selector);
            }
        })
        return (
            <div className="form-group">
                <label>Type</label>
                <div id="type" class="btn-group">
                    {selectors}
                </div>
            </div>
        );
    }

    renderNewTasksOnlyWarning() {
        if (this.hasOldValues()) {
            return (
                <div className="alert alert-info alert-slim" role="alert">
                    <strong>Note:</strong> changes made below will only affect new tasks
                </div>
            );
        } else {
            return undefined
        }
    }

    renderSlavePlacement() {
        return this.renderDropdown(
            'slavePlacement',
            'slavePlacement',
            [
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
            {
                labelText: 'Slave Placement'
            }
        );
    }

    renderInstances() {
        return this.renderBasicFormField('instances', 'instances', 'Instances', {placeholder: 1});
    }

    renderRackSensitive() {
        return this.renderCheckbox("rack-sensitive", "rackSensitive", "Rack Sensitive");
    }

    renderHideDistributeEvenlyAcrossRacksHint() {
        return this.renderCheckbox(
            "hide-distribute-evenly-across-racks-hint", 
            "hideEvenNumberAcrossRacksHint",
            "Hide Distribute Evenly Across Racks Hint"
        );
    }

    renderWaitAtLeast() {
        return this.renderBasicFormField(
            'waitAtLeast',
            'waitAtLeastMillisAfterTaskFinishesForReschedule',
            'Task rescheduling delay',
            {inputGroupAddon: 'milliseconds'}
        );
    }

    renderRackAffinity() {
        return this.renderBasicFormField(
            "rack-affinity",
            'rackAffinity',
            'Rack Affinity',
            {
                generateSelectBox: true,
                selectBoxOptions: {
                    tags: _.pluck(this.props.racks, 'id'),
                    selectOnBlur: true,
                    tokenSeparators: [',',' ']
                }
            }
        );
    }

    renderSchedule() {
        return this.renderBasicFormField(
            'schedule',
            this.getScheduleType() === 'quartzSchedule' ? "quartzSchedule" : "chronSchedule",
            'Schedule',
            {
                inputGroupAddon: this.renderDropdown(
                    'schedule-type',
                    'scheduleType',
                    [
                        {
                            value: 'cronSchedule',
                            user: 'Cron Schedule'
                        },
                        {
                            value: 'quartzSchedule',
                            user: 'Quartz Schedule'
                        }
                    ],
                    {
                        defaultChoice: this.getScheduleType(),
                        generateSelectBox: true,
                        selectBoxOptions: {containerCssClass : "select2-select-box select-box-small"}
                    }),
                inputGroupAddonExtraClasses: 'input-group-addon--select',
                required: true,
                placeholder: this.getScheduleType() === 'quartzSchedule' ? "eg: 0 */5 * * * ?" : "eg: */5 * * * *"
            }
        );
    }

    renderNumRetriesOnFailure() {
        return this.renderBasicFormField(
            'retries-on-failure',
            'numRetriesOnFailure',
            'Number of retries on failure'
        );
    }

    renderKillOldNonRunningTasks() {
        return this.renderBasicFormField(
            'killOldNRL',
            'killOldNonLongRunningTasksAfterMillis',
            'Kill cleaning task(s) after',
            {inputGroupAddon: 'milliseconds'}
        )
    }

    renderExpectedRuntimeMillis() {
        return this.renderBasicFormField(
            'expected-runtime',
            'scheduledExpectedRuntimeMillis',
            'Maximum task duration',
            {inputGroupAddon: 'milliseconds'}
        );
    }

    renderRequestTypeSpecificFormFields() {
        if (this.getValue('requestType') === 'SERVICE') {
            return(
                <div>
                    {this.renderInstances()}
                    {this.renderRackSensitive()}
                    {this.renderHideDistributeEvenlyAcrossRacksHint()}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getValue('requestType') === 'WORKER') {
            return (
                <div>
                    {this.renderInstances()}
                    {this.renderRackSensitive()}
                    {this.renderHideDistributeEvenlyAcrossRacksHint()}
                    {this.renderWaitAtLeast()}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getValue('requestType') === 'SCHEDULED') {
            return (
                <div>
                    {this.renderSchedule()}
                    {this.renderNumRetriesOnFailure()}
                    {this.renderKillOldNonRunningTasks()}
                    {this.renderExpectedRuntimeMillis()}
                </div>
            );
        } else if (this.getValue('requestType')) {
            return this.renderKillOldNonRunningTasks();
        } else if (this.getValue('requestType')) {
            return this.renderKillOldNonRunningTasks();
        }
    }

    render() {
        let requestId = this.hasOldValues() ? this.props.request.request.id : undefined;
        let labelTip = (<span className='form-label-tip'>separate multiple owners with commas</span>);
        return (
            <div className="row new-form">
                <div className="col-md-5 col-md-offset-3">
                    {this.renderHeader()}
                    <form role='form' onSubmit={event => this.submitForm(this.props, event)}>
                        {this.hasOldValues() ? undefined : this.renderId()}
                        {this.renderOwners()}
                        {this.renderRequestTypeSelectors()}
                        {this.renderNewTasksOnlyWarning()}
                        {this.renderSlavePlacement()}
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
        form: state.form[FORM_ID],
        modifications: state.form[FORM_ID] ? state.form[FORM_ID].modifications : 0
    }
}

function mapDispatchToProps(dispatch) {
    return {
        update(formId, fieldId, newValue) {
            dispatch(modifyField(formId, fieldId, newValue));
        },
        clearForm(formId) {
            dispatch(clearForm(formId));
        }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
