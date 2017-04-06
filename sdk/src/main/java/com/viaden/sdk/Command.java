package com.viaden.sdk;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Command implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<Command> CREATOR = new ParcelableCreator();
    @NonNull
    final List<Step> steps;

    private Command(@NonNull final List<Step> steps) {
        this.steps = Collections.unmodifiableList(steps);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel p, final int flags) {
        p.writeTypedList(steps);
    }

    @NonNull
    Builder newBuilder() {
        return new Builder(this);
    }

    static class Builder {
        @Nullable
        private Steps.Builder steps;

        Builder(@NonNull final JSONObject json) {
            steps = parseSteps(json);
        }

        private Builder(@NonNull final Parcel p) {
            steps = new Steps.Builder(p);
        }

        private Builder(@NonNull final Command origin) {
            steps = new Steps.Builder(origin.steps);
        }

        @Nullable
        private static Steps.Builder parseSteps(final @NonNull JSONObject json) {
            final JSONArray value = json.optJSONArray("steps");
            return value == null ? null : new Steps.Builder(value);
        }

        @NonNull
        Builder setSteps(@NonNull final List<Step> steps) {
            this.steps = new Steps.Builder(steps);
            return this;
        }

        @Nullable
        Command build() {
            if (steps == null) {
                steps = new Steps.Builder();
            }
            return new Command(steps.build());
        }
    }

    private static class ParcelableCreator implements Parcelable.Creator<Command> {
        @Nullable
        @Override
        public Command createFromParcel(@NonNull final Parcel p) {
            return new Command.Builder(p).build();
        }

        @NonNull
        @Override
        public Command[] newArray(final int size) {
            return new Command[size];
        }
    }

    private static class Steps {

        private static class Builder {
            @Nullable
            private List<Step.Builder> builders;

            private Builder() {
            }

            private Builder(@NonNull final JSONArray json) {
                final int length = json.length();
                builders = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    final JSONObject value = json.optJSONObject(i);
                    if (value != null) {
                        builders.add(new Step.Builder(value));
                    }
                }
            }

            private Builder(@NonNull final Parcel p) {
                final List<Step> steps = p.createTypedArrayList(Step.CREATOR);
                if (steps != null && !steps.isEmpty()) {
                    builders = new ArrayList<>(steps.size());
                    for (final Step step : steps) {
                        if (step != null) {
                            builders.add(step.newBuilder());
                        }
                    }
                }
            }

            private Builder(@NonNull final List<Step> steps) {
                if (!steps.isEmpty()) {
                    builders = new ArrayList<>(steps.size());
                    for (final Step step : steps) {
                        if (step != null) {
                            builders.add(step.newBuilder());
                        }
                    }
                }
            }

            @NonNull
            private List<Step> build() {
                if (builders == null || builders.isEmpty()) {
                    return Collections.emptyList();
                }
                final List<Step> steps = new ArrayList<>(builders.size());
                for (final Step.Builder builder : builders) {
                    final Step step = builder.build();
                    if (step != null) {
                        steps.add(step);
                    }
                }
                return steps;
            }
        }
    }
}
