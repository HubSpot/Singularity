import React from 'react';

class PageComponent extends React.Component {

  componentWillMount() {
    this.refresh();
  }

  componentDidMount() {
    if (this.title) {
      document.title = `${this.title} - ${config.title}`;
    } else {
      document.title = config.title;
    }
  }

  refresh() {}

  render() {
    return <div></div>;
  }
}

export default PageComponent;
