import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../../utils';

import { AdvanceDeploy } from '../../../actions/api/deploys';

import FormModal from '../modal/FormModal';

class AdvanceDeployModal extends Component {
  static propTypes = {
    deployId: PropTypes.string.isRequired,
    requestId: PropTypes.string.isRequired,
    requestParent: PropTypes.object.isRequired,
    advanceDeploy: PropTypes.func.isRequired,
    then: PropTypes.func
  };

  show() {
    this.refs.advanceModal.show();
  }

  render() {
    const targetActiveInstances = Utils.maybe(
      this.props.requestParent,
      [
        'pendingDeployState',
        'deployProgress',
        'targetActiveInstances'
      ]
    );

    const deployInstanceCountPerStep = Utils.maybe(
      this.props.requestParent,
      [
        'pendingDeployState',
        'deployProgress',
        'deployInstanceCountPerStep'
      ]
    );
    return (
      <FormModal
        name="Advance Deploy"
        ref="advanceModal"
        action="Advance Deploy"
        onConfirm={(data) => this.props.advanceDeploy(data.targetActiveInstances)}
        buttonStyle="primary"
        formElements={[
          {
            name: 'targetActiveInstances',
            min: 1,
            max: Utils.maybe(this.props.requestParent, ['request', 'instances']),
            type: FormModal.INPUT_TYPES.NUMBER,
            label: 'Number of instances:',
            defaultValue: targetActiveInstances + (deployInstanceCountPerStep || 0),
            isRequired: true
          }
        ]}>
        <p>Update the pending deploy to include this many instances (Can be higher or lower than the current target number of instances)</p>
        <pre>{this.props.deployId}</pre>
      </FormModal>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  advanceDeploy: (targetActiveInstances) => dispatch(AdvanceDeploy.trigger(
    ownProps.deployId,
    ownProps.requestId,
    targetActiveInstances
  )).then(response => (ownProps.then && ownProps.then(response)))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  null,
  { withRef: true }
)(AdvanceDeployModal);
