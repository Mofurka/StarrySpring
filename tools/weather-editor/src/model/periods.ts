import type {Period, WeatherModel} from "./types";

export const PERIOD_PALETTE = [
    "#3a6ea5",
    "#c99a3a",
    "#4aa3c9",
    "#8a6bbf",
    "#5aa38b",
    "#b0603f",
];

/** Та же арифметика, что в WeatherPeriodResolver.contains / WeatherConfigValidator.containsHour. */
export function periodContainsHour(period: Period, hour: number): boolean {
    const {fromHour: from, toHour: to} = period;
    if (from < to) return hour >= from && hour < to;
    return hour >= from || hour < to;
}

/** Сколько периодов накрывает каждый час суток; корректно ровно по одному. */
export function hourCoverage(model: WeatherModel): number[] {
    const cover = new Array<number>(24).fill(0);
    for (const period of Object.values(model.periods)) {
        for (let hour = 0; hour < 24; hour++) {
            if (periodContainsHour(period, hour)) cover[hour]++;
        }
    }
    return cover;
}

export function periodForHour(model: WeatherModel, hour: number): string | null {
    for (const [id, period] of Object.entries(model.periods)) {
        if (periodContainsHour(period, hour)) return id;
    }
    return null;
}

export function periodColor(model: WeatherModel, id: string): string {
    const keys = Object.keys(model.periods);
    const i = keys.indexOf(id);
    return PERIOD_PALETTE[(Math.max(i, 0)) % PERIOD_PALETTE.length];
}
