import React from 'react';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

let Header = React.createClass({

    render() {
        return <div>{this.props.global ? <a className='btn btn-danger' href={window.config.appRoot + '/request/' + this.props.requestId} alt={`Return to Request ${ this.props.requestId }`}><Glyphicon iconClass='arrow-left' /> Back to {this.props.requestId}</a> : undefined}<h1>{this.props.global ? 'Global' : undefined} Historical Tasks</h1></div>;
    }
});

export default Header;

