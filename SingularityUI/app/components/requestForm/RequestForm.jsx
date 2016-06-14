import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormFieldRedux';

let FORM_NAME = 'requestForm';

class RequestForm extends React.Component {

    cantSubmit() {
        return false;
    }

    submitForm() {
        // TODO - Implement
        return null;
    }

    header() {
        if (this.props.edit) {
            return <h3>Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a></h3>
        } else {
            return <h3>New Request</h3>
        }
    }

    renderForm() {
        let requestId = this.props.request.request ? this.props.request.request.id : undefined;
        return (
            <form role='form'>
                { this.props.edit ? undefined :
                    <div className="form-group required" onSubmit={this.submitForm}>
                        <label htmlFor="id">ID</label>
                        <FormField
                            id = "id"
                            className = "form-control"
                            formId = {FORM_NAME}
                            fieldId = 'requestId'
                            prop = {{
                                placeholder: "eg: my-awesome-request",
                                inputType: 'text'
                            }}
                        />
                    </div>
                }
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
        request: state.api.request ? state.api.request.data : undefined
    }
}

function mapDispatchToProps(dispatch) {
    return {
        //saveRequest: (slave) => { dispatch(FreezeAction.trigger(slave.id)); }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestForm);
