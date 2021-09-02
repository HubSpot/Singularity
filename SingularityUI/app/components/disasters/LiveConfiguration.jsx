import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';
import Section from '../common/Section';

class LiveConfiguration extends Component {
  static propTypes = {
    user: PropTypes.string,
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return (
      <Section title="Configure">
          <div style={{ display: 'flex' }}>
            <h3 style={{ margin: 0 }}>Rack Sensitivity</h3>
            <button
              className="btn btn-primary"
              alt="Enable Rack Sensitivity"
              title="Enable Rack Sensitivity">
              Enable
            </button>
            <button
              className="btn btn-danger"
              alt="Disable Rack Sensitivity"
              title="Disable Rack Sensitivity">
              Disable
            </button>
          </div>
          <div style={{ display: 'flex' }}>
              <h3 style={{ margin: 0 }}>Placement Strategy</h3>
              <input type="text" />
              <button
              className="btn btn-danger"
              alt="Override Placement Strategy"
              title="Override Placement Strategy">
              Override
            </button>
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

export default connect(mapStateToProps)(LiveConfiguration);
