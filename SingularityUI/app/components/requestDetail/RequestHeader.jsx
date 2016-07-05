import React, { PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';

import Utils from '../../utils';

import RequestStar from '../requests/RequestStar';
import RequestActionButtons from './RequestActionButtons';
import RequestAlerts from './RequestAlerts';

const RequestHeader = ({requestParent, bounces}) => {
  const {request, requestDeployState, state} = requestParent;

  return (
    <header className="detail-header">
      <Row>
        <Col md={7}>
          <h4>
            <RequestStar requestId={request.id} />
            <span className="request-state" data-state={state}>
              {Utils.humanizeText(state)}
            </span>
             <span className="request-type">
              {Utils.humanizeText(request.requestType)}
             </span>
          </h4>
          <h2>
            {request.id}
          </h2>
        </Col>
        <Col md={5} className="button-container">
          <RequestActionButtons requestParent={requestParent} />
        </Col>
      </Row>
      <Row>
        <RequestAlerts requestParent={requestParent} bounces={bounces} />
      </Row>
    </header>
  );
};

RequestHeader.propTypes = {
  requestParent: PropTypes.object.isRequired,
  bounces: PropTypes.arrayOf(PropTypes.object).isRequired
};

export default RequestHeader;
