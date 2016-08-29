import React, { PropTypes, Component } from 'react';
import { FetchDisabledActions, FetchDisastersData } from '../../actions/api/disasters';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import Utils from '../../utils';
import DisabledActions from './DisabledActions';
import ManageDisasters from './ManageDisasters';
import DisasterStats from './DisasterStats';

class Disasters extends Component {
  static propTypes = {
    disastersData: PropTypes.shape({
      stats: PropTypes.arrayOf(PropTypes.shape({
        timestamp: PropTypes.number.isRequired,
        numActiveTasks: PropTypes.number.isRequired,
        numPendingTasks: PropTypes.number.isRequired,
        numLateTasks: PropTypes.number.isRequired,
        avgTaskLagMillis: PropTypes.number.isRequired,
        numLostTasks: PropTypes.number.isRequired,
        numActiveSlaves: PropTypes.number.isRequired,
        numLostSlaves: PropTypes.number.isRequired
      })).isRequired,
      disasters: PropTypes.arrayOf(PropTypes.shape({
        type: PropTypes.string.isRequired,
        active: PropTypes.bool
      })).isRequired,
      automatedActionsDisabled: PropTypes.bool.isRequired
    }).isRequired,
    user: PropTypes.string,
    fetchDisabledActions: PropTypes.func.isRequired,
    fetchDisastersData: PropTypes.func.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return (
      <div>
        <DisabledActions disabledActions={this.props.disabledActions} user={this.props.user} />
        <ManageDisasters disasters={this.props.disastersData.disasters} user={this.props.user} automatedActionsDisabled={this.props.disastersData.automatedActionsDisabled} />
        <DisasterStats stats={this.props.disastersData.stats} />
      </div>
    );
  }
}

function mapStateToProps(state) {
  const user = Utils.maybe(state, ['api', 'user', 'data', 'user', 'name']);
  return {
    user,
    disastersData: state.api.disastersData.data,
    disabledActions: state.api.disabledActions.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchDisastersData: () => dispatch(FetchDisastersData.trigger()),
    fetchDisabledActions: () => dispatch(FetchDisabledActions.trigger())
  };
}

function refresh(props) {
	const promises = [];
	promises.push(props.fetchDisastersData());
	promises.push(props.fetchDisabledActions());
  return Promise.all(promises);
}


export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Disasters, 'Disasters', refresh));