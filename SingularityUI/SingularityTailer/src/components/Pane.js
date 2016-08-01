import React, { PropTypes } from 'react';

import 'react-virtualized/styles.css';
import '../styles/index.scss';

const Pane = ({logHeader, logComponent, logFooter}) => {
  return (
    <div>
      <section className="log-pane">
        <header>
          {logHeader}
        </header>
        <div className="log-line-wrapper">
          {logComponent}
        </div>
        <footer>
          {logFooter}
        </footer>
      </section>
    </div>
  );
};

Pane.propTypes = {
  logHeader: PropTypes.node.isRequired,
  logComponent: PropTypes.node.isRequired,
  logFooter: PropTypes.node.isRequired
};

export default Pane;
