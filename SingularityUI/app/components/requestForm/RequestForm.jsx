import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import { modifyField, clearForm } from '../../actions/form';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';

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
            return this.props.requestType;
        }
    }

    getActive(type) {
        if (this.getRequestType() === type) {
            return 'active';
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
        let key = 0;
        for (let requestType of REQUEST_TYPES) {
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
            key ++;
        }
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

    renderSlavePlacementField() {
        return (
            <div className="form-group">
                <label htmlFor="slavePlacement">Slave Placement</label>
                <DropDown
                    id = "slavePlacement"
                    prop = {{
                        updateFn: event => {
                            this.props.update(FORM_ID, 'slavePlacement', event.target.value);
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
                        ]
                    }}
                />
            </div>
        );
    }

    renderInstances() {
        return (
            <div className="form-group">
                <label htmlFor="instances">Instances</label>
                <FormField
                    id = "instances"
                    prop = {{
                        updateFn: event => {
                            this.props.update(FORM_ID, 'instances', event.target.value);
                        },
                        placeholder: "1",
                        inputType: 'text'
                    }}
                />
            </div>
        );
    }

    renderRackSensitive() {
        return (
            <div className="form-group">
                <label htmlFor="rack-sensitive" className="control-label">
                    Rack Sensitive
                    <FormField
                        id = "rack-sensitive"
                        prop = {{
                            updateFn: event => {
                                this.props.update(FORM_ID, 'rackSensitive', !(this.props.form.rackSensitive));
                            },
                            inputType: 'checkBox'
                        }}
                    />
                </label>
            </div>
        );
    }

    renderHideDistributeEvenlyAcrossRacksHint() {
        return (
            <div className="form-group">
                <label htmlFor="hide-distribute-evenly-across-racks-hint" className="control-label">
                    Hide Distribute Evenly Across Racks Hint
                    <FormField
                        id = "hide-distribute-evenly-across-racks-hint"
                        className = "hide-distribute-evenly-across-racks-hint-checkbox"
                        prop = {{
                            onClick: event => {
                                this.props.update(FORM_ID, 'hideDistributEvenlyAcrossRacksHint', !(this.props.form.hideDistributEvenlyAcrossRacksHint));
                            },
                            inputType: 'checkBox'
                        }}
                    />
                </label>
            </div>
        );
    }

    renderLoadBalanced() {
        return (
            <div className="form-group">
                <label htmlFor="load-balanced" className="control-label">
                    Load Balanced
                    <FormField
                        id = "load-balanced"
                        prop = {{
                            updateFn: event => {
                                this.props.update(FORM_ID, 'loadBalanced', !(this.props.form.loadBalanced));
                            },
                            inputType: 'checkBox'
                        }}
                    />
                </label>
            </div>
        );
    }

    renderTaskReschedulingDelay() {
        return (
            <div className="form-group">
                <label for="waitAtLeast">Task rescheduling delay</label>
                <div className="input-group">
                    <FormField
                        id = "waitAtLeast"
                        prop = {{
                            updateFn: event => {
                                this.props.update(FORM_ID, 'waitAtLeast', event.target.value);
                            },
                            inputType: 'text'
                        }}
                    />
                    <div className="input-group-addon">milliseconds</div>
                </div>
            </div>
        );
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
                    {this.renderInstances()}
                    {this.renderRackSensitive()}
                    {this.renderHideDistributeEvenlyAcrossRacksHint()}
                    {config.loadBalancingEnabled ? this.renderLoadBalanced() : undefined}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getRequestType() === 'WORKER') {
            return (
                <div>
                    {this.renderInstances()}
                    {this.renderRackSensitive()}
                    {this.renderHideDistributeEvenlyAcrossRacksHint()}
                    {this.renderTaskReschedulingDelay()}
                    {this.renderRackAffinity()}
                </div>
            );
        } else if (this.getRequestType() === 'SCHEDULED') {
            //
        } else if (this.getRequestType() === 'ON_DEMAND') {
            return this.renderTaskReschedulingDelay();
        } else if (this.getRequestType() === 'RUN_ONCE') {
            return this.renderTaskReschedulingDelay();
        }
    }

    renderForm() {
        let requestId = this.props.request.request ? this.props.request.request.id : undefined;
        return (
            <form role='form' onSubmit={event => this.submitForm(this.props, event)}>
                { this.props.edit ? undefined :
                    <div className="form-group required" onSubmit={this.submitForm}>
                        <label htmlFor="id">ID</label>
                        <FormField
                            id = "id"
                            className = "form-control"
                            prop = {{
                                updateFn: event => {
                                    this.props.update(FORM_ID, 'requestId', event.target.value);
                                },
                                placeholder: "eg: my-awesome-request",
                                inputType: 'text'
                            }}
                        />
                    </div>
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
                {this.renderSlavePlacementField()}
                {this.renderRequestTypeSpecificFormFields()}
                <div id="button-row">
                    <span>
                        <button type="submit" className="btn btn-success btn-lg" disabled={this.cantSubmit() ? 'disabled' : undefined}>
                            Save
                        </button>
                    </span>
                </div>
            </form>
        );
    }

    render() {
        return(
            <div className="row new-form">
                <div className="col-md-5 col-md-offset-3">
                    {this.header()}
                    {this.renderForm()}
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
        requestType: state.form && state.form[FORM_ID] ? state.form[FORM_ID].requestType : undefined
    }
}

function mapDispatchToProps(dispatch) {
    return {
        update: (formId, fieldId, newValue) => { dispatch(modifyField(formId, fieldId, newValue)); },
        clearForm: (formId) => { dispatch(clearForm(formId)); }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
