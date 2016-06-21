import React from 'react';
import moment from 'moment';
import Utils from '../../../utils';

let TimeStamp = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            timestamp: React.PropTypes.number.isRequired,
            className: React.PropTypes.string,
            display: React.PropTypes.string.isRequired,
            postfix: React.PropTypes.string,
            prefix: React.PropTypes.string
        }).isRequired
    },

    render() {
        let formatted = '';
        // Feel free to add more options if you need them
        if (this.props.prop.display === 'timeStampFromNow') {
            formatted = Utils.timeStampFromNow(this.props.prop.timestamp);
        } else if (this.props.prop.display === 'absoluteTimestamp') {
            formatted = Utils.absoluteTimestamp(this.props.prop.timestamp);
        } else if (this.props.prop.display === 'duration') {
            formatted = Utils.duration(this.props.prop.timestamp);
        }
        return <div className={this.props.prop.className}>{`${this.props.prop.prefix || ''} ${formatted} ${this.props.prop.postfix || ''}`}</div>;
    }
});

export default TimeStamp;
