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
        if (this.props.edit && this.getRequestType() !== type) {
            return 'disabled';
        } else {
            return null;
        }
    }

    updateTypeButtonClick(props, event) {
        event.preventDefault();
        props.update(FORM_ID, 'requestType', event.target.value);
    }

    getRequestType() {
        if (this.props.edit) {
            return this.props.request.request.requestType;
        } else {
            return this.props.form ? this.props.form.requestType : undefined;
        }
    }

    getActive(type) {
        if (this.getRequestType() === type) {
            return 'active';
        }
    }

    renderBasicFormField(htmlId, fieldId, labelText, {placeholder, inputGroupAddon, inputGroupAddonExtraClasses, required} = {}) {
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
                            value: this.props.form ? this.props.form[fieldId] : ""
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
                                this.props.update(FORM_ID, fieldId, !(this.props.form[fieldId]));
                            },
                            inputType: 'checkBox',
                            checked: this.props.form[fieldId]
                        }}
                    />
                </label>
            </div>
        );
    }

    renderDropdown(htmlId, fieldId, choices, {defaultChoice, generateSelectBox, selectBoxOptions, labelText} = {}) {
        let dropDown = (
            <DropDown
                id = {htmlId}
                prop = {{
                    updateFn: event => {
                        this.props.update(FORM_ID, fieldId, event.target.value);
                    },
                    forceChooseValue: true,
                    choices: choices,
                    value: this.props.form ? this.props.form[fieldId] : defaultChoice,
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

    header() {
        if (this.props.edit) {
            return <h3>Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a></h3>
        } else {
            return <h3>New Request</h3>
        }
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
            if (this.props.edit && requestType === this.getRequestType()) {
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
        if (this.props.edit) {
            return (
                <div className="alert alert-info alert-slim" role="alert">
                    <strong>Note:</strong> changes made below will only affect new tasks
                </div>
            );
        } else {
            return undefined
        }
    }

    renderRackAffinity() {
        return (
            <div className="form-group">
                <label htmlFor="rack-affinity">Rack Affinity</label>
                <FormField
                    id = "rack-affinity"
                    className = "tagging-input"
                    prop = {{
                        updateFn: event => {
                            this.props.update(FORM_ID, 'rackAffinity', event.target.value);
                        },
                        inputType: 'text'
                    }}
                />
            </div>
        );
    }

    renderRequestTypeSpecificFormFields() {
        if (this.getRequestType() === 'SERVICE') {
            return(
                <div>
                    {this.renderBasicFormField('instances', 'instances', 'Instances', {placeholder: 1})}
                    {this.renderCheckbox("rack-sensitive", "rackSensitive", "Rack Sensitive")}
                    {this.renderCheckbox(
                        "hide-distribute-evenly-across-racks-hint", 
                        "hideDistributEvenlyAcrossRacksHint",
                        "Hide Distribute Evenly Across Racks Hint")}
                    {config.loadBalancingEnabled ? this.renderCheckbox("load-balanced", "loadBalanced", "Load balanced") : undefined}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getRequestType() === 'WORKER') {
            return (
                <div>
                    {this.renderBasicFormField('instances', 'instances', 'Instances', {placeholder: 1})}
                    {this.renderCheckbox("rack-sensitive", "rackSensitive", "Rack Sensitive")}
                    {this.renderCheckbox(
                        "hide-distribute-evenly-across-racks-hint", 
                        "hideDistributEvenlyAcrossRacksHint",
                        "Hide Distribute Evenly Across Racks Hint")}
                    {this.renderBasicFormField('waitAtLeast', 'waitAtLeast', 'Task rescheduling delay', {inputGroupAddon: 'milliseconds'})}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getRequestType() === 'SCHEDULED') {
            return (
                <div>
                    {this.renderBasicFormField(
                        'schedule',
                        'schedule',
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
                                    defaultChoice: 'cronSchedule',
                                    generateSelectBox: true,
                                    selectBoxOptions: {containerCssClass : "select2-select-box select-box-small"}
                                }),
                            inputGroupAddonExtraClasses: 'input-group-addon--select',
                            required: true,
                            placeholder: this.props.form && this.props.form.scheduleType === 'quartzSchedule' ? 
                                "eg: 0 */5 * * * ?" : 
                                "eg: */5 * * * *"
                        }
                    )}
                    {this.renderBasicFormField(
                        'retries-on-failure',
                        'retriesOnFailure',
                        'Number of retries on failure'
                    )}
                    {this.renderBasicFormField(
                        'killOldNRL',
                        'killCleaningTasksAfter',
                        'Kill cleaning task(s) after',
                        {inputGroupAddon: 'milliseconds'}
                    )}
                    {this.renderBasicFormField(
                        'expected-runtime',
                        'expectedRuntime',
                        'Maximum task duration',
                        {inputGroupAddon: 'milliseconds'}
                    )}
                </div>
            );
        } else if (this.getRequestType() === 'ON_DEMAND') {
            return this.renderBasicFormField(
                'killOldNRL',
                'killCleaningTasksAfter',
                'Kill cleaning task(s) after',
                {inputGroupAddon: 'milliseconds'}
            );
        } else if (this.getRequestType() === 'RUN_ONCE') {
            return this.renderBasicFormField(
                'killOldNRL',
                'killCleaningTasksAfter',
                'Kill cleaning task(s) after',
                {inputGroupAddon: 'milliseconds'}
            );
        }
    }

    render() {
        let requestId = this.props.request.request ? this.props.request.request.id : undefined;
        return (
            <div className="row new-form">
                <div className="col-md-5 col-md-offset-3">
                    <form role='form' onSubmit={event => this.submitForm(this.props, event)}>
                        { this.props.edit ? undefined : this.renderBasicFormField(
                            "id",
                            "requestId",
                            "ID",
                            {
                                placeholder: "eg: my-awesome-request",
                                required: true
                            })
                        }
                        <div class="form-group">
                            <label htmlFor="owner">Owners <span className='form-label-tip'>separate multiple owners with commas</span></label>
                            <FormField
                                    id = "owners"
                                    className = "tagging-input"
                                    prop = {{
                                        updateFn: event => {
                                            this.props.update(FORM_ID, 'owners', event.target.value);
                                        },
                                        inputType: 'text'
                                    }}
                                />
                        </div>
                        {this.renderRequestTypeSelectors()}
                        {this.renderNewTasksOnlyWarning()}
                        {this.renderDropdown(
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
                            )
                        }
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
        update: (formId, fieldId, newValue) => { dispatch(modifyField(formId, fieldId, newValue)); },
        clearForm: (formId) => { dispatch(clearForm(formId)); }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
