from setuptools import setup, find_packages

requirements = [
  'argparse',
  'ConfigParser',
  'grequests',
  'requests',
  'termcolor'
]

setup(
    name='logfetch',
    version='0.0.1',
    description='Singularity log fetching and searching',
    author='Hubspot PaaS',
    url='https://github.com/HubSpot/Singularity',
    packages=find_packages(),
    include_package_data=True,
    install_requires=requirements,
    zip_safe=False,
    entry_points={
        'console_scripts':['logfetch=logfetch.log_fetcher:entrypoint'],
    }
)
