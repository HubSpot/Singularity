import React from 'react';
import moment from 'moment';

let TimeStamp = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            timestamp: React.PropTypes.number.isRequired,
            className: React.PropTypes.string,
            display: React.PropTypes.string.isRequired
        }).isRequired
    },

    timeStampFromNow() {
        let timeObject = moment(this.props.prop.timestamp);
        return <div className={this.props.prop.className}>{timeObject.fromNow()} ({timeObject.format(window.config.timestampFormat)})</div>;
    },

    absoluteTimestamp() {
        let timeObject = moment(this.props.prop.timestamp);
        return <div className={this.props.prop.className}>{timeObject.format(window.config.timestampFormat)}</div>;
    },

    duration() {
        return <div className={this.props.prop.className}>{moment.duration(this.props.prop.timestamp).humanize()}</div>;
    },

    render() {
        // Feel free to add more options if you need them
        if (this.props.prop.display === 'timeStampFromNow') {
            return this.timeStampFromNow();
        } else if (this.props.prop.display === 'absoluteTimestamp') {
            return this.absoluteTimestamp();
        } else if (this.props.prop.display === 'duration') {
            return this.duration();
        }
    }
});

export default TimeStamp;

