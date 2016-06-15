import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormFieldRedux';
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
        event.preventDefault()
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

    renderRequestTypeSpecificFormFields() {
        //
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
                            formId = {FORM_ID}
                            fieldId = 'requestId'
                            prop = {{
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
                            formId = {FORM_ID}
                            fieldId = 'owners'
                            prop = {{
                                inputType: 'text'
                            }}
                        />
                </div>
                {this.renderRequestTypeSelectors()}
                {this.renderNewTasksOnlyWarning()}
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
