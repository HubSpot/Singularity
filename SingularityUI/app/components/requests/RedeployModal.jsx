import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Alert } from 'react-bootstrap';
import JSONTree from 'react-json-tree';
import { JSONTreeTheme } from '../../thirdPartyConfigurations';

import { SaveDeploy } from '../../actions/api/deploys';

import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';

class RedeployModal extends Component {
  static propTypes = {
    unpauseRequest: PropTypes.func,
    deploy: PropTypes.object
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  show() {
    this.refs.redeployModal.show();
  }

  confirmRedeploy(data) {
    this.props.unpauseRequest(data).then((response) => {
      if (response.statusCode !== 200) {
        this.setState({error: response.error, errorCode: response.statusCode});
        this.show();
      }
    });
  }

  render() {
    const deployId = Utils.maybe(this.props.deploy, ['deployMarker', 'deployId']);
    return (
      <span>
        <FormModal
          ref="redeployModal"
          action="Redeploy"
          onConfirm={(data) => this.confirmRedeploy(data)}
          buttonStyle="primary"
          formElements={[
            {
              name: 'deployId',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'New Deploy Id (Must not be the same Id as the old deploy, or any other deploy)',
              defaultValue: deployId,
              isRequired: true,
              validateField: (newDeployId) => newDeployId === deployId && 'New deploy id must not be the same as the old deploy id'
            }
          ]}>
          {this.state.error && (
            <Alert bsStyle="danger">
              Failed to redeploy {deployId}. The server responded with a <code>HTTP {this.state.errorCode}</code> and said:
              <pre>{this.state.error}</pre>
            </Alert>
          )}
          <p>Are you sure you want to redeploy this deploy?</p>
          <pre>{deployId}</pre>
          <p>This will create a new deploy with the same attributes as the old deploy:</p>
          <JSONTree
            data={{deploy: _.omit(Utils.maybe(this.props.deploy, ['deploy']), 'id')}}
            hideRoot={true}
            theme={JSONTreeTheme}
          />
        </FormModal>
      </span>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  unpauseRequest: (data) => dispatch(SaveDeploy.trigger({deploy: _.extend({}, ownProps.deploy.deploy, {id: data.deployId})})),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RedeployModal);
