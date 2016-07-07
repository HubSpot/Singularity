import React from 'react';

const rootComponent = (title, refresh, Wrapped) => class extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      loading: typeof refresh === 'function'
    };
  }

  componentDidMount() {
    if (refresh) {
      const promise = refresh(this.props);
      if (promise) {
        promise.then(() => {
          this.setState({
            loading: false
          });
        });
      }
    }
    document.title = `${title} - ${config.title}`;
  }

  render() {
    const loader = this.state.loading && <div className="page-loader fixed" />;
    const page = !this.state.loading && <Wrapped {...this.props} />;
    return (
      <div>
        {loader}
        {page}
      </div>
    );
  }
};

export default rootComponent;
