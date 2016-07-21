import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';

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

class RequestTitle extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    requestAPI: PropTypes.object
  };

  constructor(props) {
    super(props);
    this.state = {hover: false};
    this.onMouseOver = this.onMouseOver.bind(this);
  }

  onMouseOver() {
    this.setState({ hover: true });
  }

  render() {
    const {requestAPI, requestId} = this.props;
    const {hover} = this.state;
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

    const requestIdToDisplay = Utils.maybe(requestAPI, ['data', 'request', 'id']) || requestId;

    return (
      <div onMouseOver={this.onMouseOver}>
        <h4>
          {maybeInfo}
        </h4>
        <h2>
            {hover && <span><a className="copy-btn" data-clipboard-text={requestIdToDisplay}>Copy</a> </span>}
            {requestIdToDisplay}
        </h2>
      </div>
    );
  }
}

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
