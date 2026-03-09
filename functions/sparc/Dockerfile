# Use the official Python image.
# https://hub.docker.com/_/python
FROM python:3.10-slim

# Allow statements and log messages to immediately appear in the Knative logs
ENV PYTHONUNBUFFERED True

# Install specific system dependencies for scipy/numpy/pandas
RUN apt-get update && apt-get install -y \
    build-essential \
    libopenblas-dev \
    liblapack-dev \
    gfortran \
    && rm -rf /var/lib/apt/lists/*

# Copy local code to the container image.
ENV APP_HOME /app
WORKDIR $APP_HOME

# Copy requirements first to leverage Docker cache
COPY requirements.txt ./

# Install production dependencies.
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the code
COPY . ./

# Run the web service on container startup.
# --target must match the function name in main.py
# --signature-type must match the function type (cloudevent for Eventarc triggers)
CMD exec functions-framework --target=process_sparc --signature-type=cloudevent
