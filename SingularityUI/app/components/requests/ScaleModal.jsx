import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import {
  ScaleRequest,
  BounceRequest
} from '../../actions/api/requests';

import FormModal from '../common/FormModal';

class ScaleModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    scaleRequest: PropTypes.func.isRequired,
    bounceRequest: PropTypes.func.isRequired,
    currentInstances: PropTypes.number
  };

  // NOTE: currentInstances
  // because there are many different places where the request object could be
  // in the store, we don't try to mapStateToProps this

  static INCREMENTAL_BOUNCE_VALUE = {
    INCREMENTAL: {
      label: 'Kill old tasks as new tasks become healthy',
      value: true
    },
    ALL: {
      label: 'Kill old tasks once ALL new tasks are healthy',
      value: false
    }
  };

  handleScale(data) {
    const { instances, durationMillis, message } = data;
    this.props.scaleRequest(
      {
        instances,
        durationMillis,
        message
      }
    ).then((r) => console.log(r));
    if (data.bounce) {
      this.props.bounceRequest(
        {
          incremental: !!data.incremental
        }
      ).then((r) => console.log(r));
    }
  }

  show() {
    this.refs.scaleModal.show();
  }

  render() {
    return (
      <FormModal
        ref="scaleModal"
        action="Scale Request"
        onConfirm={(data) => this.handleScale(data)}
        buttonStyle="primary"
        formElements={[
          {
            name: 'instances',
            min: 1,
            type: FormModal.INPUT_TYPES.NUMBER,
            label: 'Number of instances:',
            defaultValue: this.props.currentInstances,
            isRequired: true
          },
          {
            name: 'bounce',
            type: FormModal.INPUT_TYPES.BOOLEAN,
            label: 'Bounce after scaling',
            defaultValue: false
          },
          {
            name: 'incremental',
            type: FormModal.INPUT_TYPES.RADIO,
            values: _.values(ScaleModal.INCREMENTAL_BOUNCE_VALUE),
            dependsOn: 'bounce',
            defaultValue: ScaleModal.INCREMENTAL_BOUNCE_VALUE.INCREMENTAL.value
          },
          {
            name: 'durationMillis',
            type: FormModal.INPUT_TYPES.DURATION,
            label: 'Expiration: (optional)'
          },
          {
            name: 'message',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Message: (optional)'
          }
        ]}>
        <p>Scaling request:</p>
        <pre>{this.props.requestId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  scaleRequest: (data) => dispatch(ScaleRequest.trigger(ownProps.requestId, data)),
  bounceRequest: (data) => dispatch(BounceRequest.trigger(ownProps.requestId, data))
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(ScaleModal);
