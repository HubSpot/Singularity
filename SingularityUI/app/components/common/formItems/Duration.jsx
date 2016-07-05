import React from 'react';

import moment from 'moment';

export default class Duration extends React.Component {

  handleChange(event) {
    event.preventDefault();
    const duration = moment.duration(`${this.refs.hours.value || 0}:${this.refs.minutes.value || 0}:${this.refs.seconds.value || 0}`);
    this.props.onChange(duration.asMilliseconds());
  }

  render() {
    const duration = moment.duration(this.props.value);
    return (
      <div className="form-inline duration-input row">
        <div className="form-group col-md-4">
          <label for="hours">Hours</label>
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
        <div className="form-group col-md-4">
          <label for="minutes">Minutes</label>
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
        <div className="form-group col-md-4">
          <label for="seconds">Seconds</label>
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

Duration.PropTypes = {
  value: React.PropTypes.number, // Duration in millis
  onChange: React.PropTypes.func
}
