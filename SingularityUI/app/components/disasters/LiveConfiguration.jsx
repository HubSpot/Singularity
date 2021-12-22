import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import Messenger from 'messenger';
import Utils from '../../utils';
import Section from '../common/Section';
import { EnableRackSensitivity, DisableRackSensitivity, OverridePlacementStrategy } from '../../actions/api/config';

class LiveConfiguration extends Component {
  static propTypes = {
    user: PropTypes.string,
    enableRackSensitivity: PropTypes.func,
    disableRackSensitivity: PropTypes.func,
    overridePlacementStrategy: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = {
      placementStrategy: '',
    };
    this.updatePlacementStrategy = e => {
      this.setState({ placementStrategy: e.target.value });
    }
  }

  render() {
    return (
      <Section title="Configure">
        <div className="row">
          <div className="col-md-6">
            <h3>Rack Sensitivity</h3>
            <button
              className="btn btn-primary"
              alt="Enable Rack Sensitivity"
              title="Enable Rack Sensitivity"
              onClick={this.props.enableRackSensitivity}>
              Enable
            </button>
            <button
              className="btn btn-danger"
              alt="Disable Rack Sensitivity"
              title="Disable Rack Sensitivity"
              onClick={this.props.disableRackSensitivity}>
              Disable
            </button>
          </div>
          <div className="col-md-6">
            <h3>Placement Strategy</h3>
            <input
              type="text"
              placeholder="GREEDY/OPTIMISTIC"
              onChange={this.updatePlacementStrategy}
              value={this.state.placementStrategy}
            />
            <button
              className="btn btn-danger"
              alt="Override Placement Strategy"
              title="Override Placement Strategy"
              onClick={() => this.props.overridePlacementStrategy(this.state.placementStrategy)}>
              Override
            </button>
            <button
              className="btn btn-primary"
              alt="Clear Placement Strategy Override"
              title="Clear Placement Strategy Override"
              onClick={() => this.props.overridePlacementStrategy('')}>
              Clear
            </button>
          </div>
        </div>
      </Section>
    );
  }
}

function mapStateToProps(state) {
  const user = Utils.maybe(state, ['api', 'user', 'data', 'user', 'name']);
  return {
    user
  };
}

function mapDispatchToProps(dispatch) {
  return {
    enableRackSensitivity: () => dispatch(EnableRackSensitivity.trigger()).then(() => {
      Messenger().info({
        message: `Enabled rack sensitivity.`
      });
    }),
    disableRackSensitivity: () => dispatch(DisableRackSensitivity.trigger()).then(() => {
      Messenger().info({
        message: `Disabled rack sensitivity.`
      });
    }),
    overridePlacementStrategy: strategy => dispatch(OverridePlacementStrategy.trigger(strategy)).then(() => {
      const message = strategy
        ? `Set default placement strategy to ${strategy}.`
        : `Cleared default placement strategy override`;

      Messenger().info({
        message
      });
    }),
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(LiveConfiguration);
