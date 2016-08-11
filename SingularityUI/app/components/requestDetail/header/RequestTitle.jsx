import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { OverlayTrigger, Popover } from 'react-bootstrap';

import Utils from '../../../utils';

import RequestStar from '../../requests/RequestStar';

const errorDescription = (requestAPI) => {
  switch (requestAPI.statusCode) {
    case 404:
      return 'Request not found';
    case 401:
      return 'Not authorized';
    default:
      return requestAPI.error;
  }
};

const RequestTitle = ({requestId, requestAPI}) => {
  let maybeInfo;
  if (Utils.api.isFirstLoad(requestAPI)) {
    maybeInfo = <em>Loading...</em>;
  } else if (requestAPI.error) {
    const errorText = errorDescription(requestAPI);
    maybeInfo = <p className="text-danger">{requestAPI.statusCode}: {errorText}</p>;
  } else {
    const requestParent = requestAPI.data;
    const {request, state} = requestParent;
    maybeInfo = (
      <span>
        <RequestStar requestId={request.id} />
        <span className="request-state" data-state={state}>
          {Utils.humanizeText(state)}
        </span>
        <span className="request-type">
          {Utils.humanizeText(request.requestType)}
        </span>
      </span>
    );
  }

  const copyLinkPopover = (
    <Popover id="popover-trigger-focus">
      Click to copy
    </Popover>
  );

  return (
    <div>
      <h4>
        {maybeInfo}
      </h4>
      <h2>
        <OverlayTrigger trigger={['hover', 'focus', 'click']} placement="left" overlay={copyLinkPopover}>
          <span className="copy-btn" data-clipboard-text={requestId}>{requestId}</span>
        </OverlayTrigger>
      </h2>
    </div>
  );
};

RequestTitle.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestAPI: PropTypes.object
};

const mapStateToProps = (state, ownProps) => ({
  requestAPI: Utils.maybe(state.api.request, [ownProps.requestId])
});

export default connect(
  mapStateToProps
)(RequestTitle);
