import React from 'react';
import { connect } from 'react-redux';

class RequestForm extends React.Component {

    render() {
        return(
            <h1> Hello World </h1>
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
