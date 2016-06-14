import React from 'react';
import { connect } from 'react-redux';

class RequestForm extends React.Component {

    header() {
        if (this.props.edit) {
            return <h3>Editing <a href={`${config.appRoot}/request/${this.props.request.request.id}`}>{this.props.request.request.id}</a></h3>
        } else {
            return <h3>New Request</h3>
        }
    }

    render() {
        return(
            <div className="row new-form">
                <div className="col-md-5 col-md-offset-3">
                    {this.header()}
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
