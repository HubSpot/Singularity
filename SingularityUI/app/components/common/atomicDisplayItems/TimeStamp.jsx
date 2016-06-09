import React from 'react';
import moment from 'moment';

let TimeStamp = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            timestamp: React.PropTypes.number.isRequired,
            className: React.PropTypes.string,
            display: React.PropTypes.string.isRequired,
            postfix: React.PropTypes.string
        }).isRequired
    },

    timeStampFromNow() {
        let timeObject = moment(this.props.prop.timestamp);
        return `${timeObject.fromNow()} (${timeObject.format(window.config.timestampFormat)})`;
    },

    absoluteTimestamp() {
        let timeObject = moment(this.props.prop.timestamp);
        return timeObject.format(window.config.timestampFormat);
    },

    duration() {
        return moment.duration(this.props.prop.timestamp).humanize();
    },

    render() {
        let formatted = '';
        // Feel free to add more options if you need them
        if (this.props.prop.display === 'timeStampFromNow') {
            formatted = this.timeStampFromNow();
        } else if (this.props.prop.display === 'absoluteTimestamp') {
            formatted = this.absoluteTimestamp();
        } else if (this.props.prop.display === 'duration') {
            formatted = this.duration();
        }
        return <div className={this.props.prop.className}>{`${formatted} ${this.props.prop.postfix || ''}`}</div>;
    }
});

export default TimeStamp;
