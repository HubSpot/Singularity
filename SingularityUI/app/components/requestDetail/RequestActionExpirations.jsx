import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

const RequestActionExpirations = ({requestParent}) => {
  if (requestParent.expiringScale) {
    const {
      expiringScale: {
        startMillis,
        expiringAPIRequestObject: {
          durationMillis
        }
      }
    } = requestParent;

    if (startMillis + durationMillis > new Date().getTime()) {
      return <div>expiringScale</div>;
    }
  }


  return <span />;
};

RequestActionExpirations.propTypes = {
  requestParent: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data'])
});

export default connect(
  mapStateToProps,
  null
)(RequestActionExpirations);
