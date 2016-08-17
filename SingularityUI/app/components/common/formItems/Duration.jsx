import React, { PropTypes } from 'react';

import moment from 'moment';
import classNames from 'classnames';

export default class Duration extends React.Component {

  static propTypes = {
    value: PropTypes.number, // Duration in millis
    onChange: PropTypes.func,
    isSubForm: PropTypes.bool
  };

  handleChange(event) {
    event.preventDefault();
    const newHours = this.refs.hours.value || 0;
    if (newHours >= 24) {
      return;
    }
    const newMinutes = this.refs.minutes.value || 0;
    if (newMinutes >= 60) {
      return;
    }
    const newSeconds = this.refs.seconds.value || 0;
    if (newSeconds >= 60) {
      return;
    }
    const duration = moment.duration(`${newHours}:${newMinutes}:${newSeconds}`);
    this.props.onChange(duration.asMilliseconds());
  }

  render() {
    const duration = moment.duration(this.props.value);
    return (
      <div className={classNames('duration-input', 'row', {'form-inline': !this.props.isSubForm})}>
        <div className={classNames('col-md-4', {'form-group': !this.props.isSubForm})}>
          <label htmlFor="hours">Hours</label>
          <input
            id="hours"
            ref="hours"
            className="form-control"
            type="number"
            min={0}
            step={1}
            value={this.props.value ? duration.hours() : ''}
            onChange={(e) => this.handleChange(e)}
          />
        </div>
        <div className={classNames('col-md-4', {'form-group': !this.props.isSubForm})}>
          <label htmlFor="minutes">Minutes</label>
          <input
            id="minutes"
            ref="minutes"
            className="form-control"
            type="number"
            min={0}
            step={1}
            value={this.props.value ? duration.minutes() : ''}
            onChange={(e) => this.handleChange(e)}
          />
        </div>
        <div className={classNames('col-md-4', {'form-group': !this.props.isSubForm})}>
          <label htmlFor="seconds">Seconds</label>
          <input
            id="seconds"
            ref="seconds"
            className="form-control"
            type="number"
            min={0}
            step={1}
            value={this.props.value ? duration.seconds() : ''}
            onChange={(e) => this.handleChange(e)}
          />
        </div>
      </div>
    );
  }
}
