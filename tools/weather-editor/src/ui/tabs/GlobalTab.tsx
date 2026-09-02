import {useDerived} from "../../state/derived";
import {SectionHead} from "../controls";
import {TransitionList} from "../TransitionList";

export function GlobalTab() {
    const {model} = useDerived();

    return (
        <>
            <SectionHead
                title="Глобальные переходы"
                lead="Кандидаты «из любого состояния». Добавляются к переходам текущего состояния при подсчёте весов — удобно для редких событий (туман, жара) без прописывания в каждом состоянии."
            />
            <div className="panel" style={{padding: 14}}>
                <TransitionList list={model.globalTransitions}/>
            </div>
        </>
    );
}
