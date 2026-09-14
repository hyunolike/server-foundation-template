#!/usr/bin/env bash
#
# 템플릿을 복제한 저장소를 자기 서비스 이름으로 바꾼다.
#
#   ./scripts/init-template.sh --module order-api --package com.acme.order [--group com.acme]
#
# 하는 일: sample-api 모듈의 이름·패키지·클래스 접두사와 Gradle group 을 바꾼다.
# 하지 않는 일: foundation-* 모듈은 건드리지 않는다 — 공유 라이브러리의 패키지다.
set -euo pipefail

MODULE=""
PACKAGE=""
GROUP=""
DRY_RUN=false

usage() {
    sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --module)  MODULE="$2"; shift 2 ;;
        --package) PACKAGE="$2"; shift 2 ;;
        --group)   GROUP="$2"; shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        -h|--help) usage 0 ;;
        *) echo "알 수 없는 인자: $1" >&2; usage 1 ;;
    esac
done

[[ -n "$MODULE"  ]] || { echo "--module 이 필요합니다." >&2; usage 1; }
[[ -n "$PACKAGE" ]] || { echo "--package 가 필요합니다." >&2; usage 1; }
[[ "$MODULE"  =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]] || { echo "모듈 이름은 소문자-하이픈 형식이어야 합니다: $MODULE" >&2; exit 1; }
[[ "$PACKAGE" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$ ]] || { echo "패키지가 올바르지 않습니다: $PACKAGE" >&2; exit 1; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OLD_MODULE="sample-api"
OLD_PACKAGE="com.hyunolike.sample"
OLD_TEST_PACKAGE="com.hyunolike.sampletest"
OLD_GROUP="com.hyunolike.foundation"
OLD_PREFIX="SampleApi"
OLD_TITLE="Sample API"

[[ -d "$OLD_MODULE" ]] || { echo "$OLD_MODULE 이 없습니다. 이미 실행한 저장소일 수 있습니다." >&2; exit 1; }

# order-api -> OrderApi
NEW_PREFIX="$(echo "$MODULE" | awk -F- '{ for (i = 1; i <= NF; i++) printf toupper(substr($i, 1, 1)) substr($i, 2); }')"
# 테스트 전용 컨트롤러는 컴포넌트 스캔 밖에 있어야 한다 — 형제 패키지로 둔다.
PARENT_PACKAGE="${PACKAGE%.*}"
NEW_TEST_PACKAGE="${PARENT_PACKAGE}.testsupport"
GROUP="${GROUP:-$PARENT_PACKAGE}"
# order-api -> Order API
NEW_TITLE="$(echo "$MODULE" | awk -F- '{ for (i = 1; i <= NF; i++) { w = toupper(substr($i, 1, 1)) substr($i, 2); if (tolower($i) == "api") w = "API"; printf (i > 1 ? " " : "") w; } }')"

echo "모듈     : $OLD_MODULE -> $MODULE"
echo "패키지   : $OLD_PACKAGE -> $PACKAGE"
echo "테스트   : $OLD_TEST_PACKAGE -> $NEW_TEST_PACKAGE"
echo "클래스   : ${OLD_PREFIX}* -> ${NEW_PREFIX}*"
echo "문서 제목: $OLD_TITLE -> $NEW_TITLE"
echo "group    : $OLD_GROUP -> $GROUP"
$DRY_RUN && { echo "(dry-run: 아무것도 바꾸지 않았습니다)"; exit 0; }

move() { mkdir -p "$(dirname "$2")"; git mv "$1" "$2" 2>/dev/null || mv "$1" "$2"; }

# 1) 모듈 디렉터리
move "$OLD_MODULE" "$MODULE"

# 2) 패키지 디렉터리
for SOURCE_SET in main test; do
    SRC="$MODULE/src/$SOURCE_SET/kotlin/${OLD_PACKAGE//./\/}"
    [[ -d "$SRC" ]] && move "$SRC" "$MODULE/src/$SOURCE_SET/kotlin/${PACKAGE//./\/}"
done
TEST_SRC="$MODULE/src/test/kotlin/${OLD_TEST_PACKAGE//./\/}"
[[ -d "$TEST_SRC" ]] && move "$TEST_SRC" "$MODULE/src/test/kotlin/${NEW_TEST_PACKAGE//./\/}"

find "$MODULE/src" -type d -empty -delete

# 3) 클래스 파일 이름
while IFS= read -r FILE; do
    [[ -n "$FILE" ]] || continue
    move "$FILE" "$(dirname "$FILE")/$(basename "$FILE" | sed "s/^${OLD_PREFIX}/${NEW_PREFIX}/")"
done < <(find "$MODULE" -type f -name "${OLD_PREFIX}*.kt")

# 4) 텍스트 치환
while IFS= read -r FILE; do
    [[ -f "$FILE" ]] || continue
    perl -pi -e "s/\Q${OLD_TITLE}\E/${NEW_TITLE}/g;
                 s/\Q${OLD_TEST_PACKAGE}\E/${NEW_TEST_PACKAGE}/g;
                 s/\Q${OLD_PACKAGE}\E/${PACKAGE}/g;
                 s/\b\Q${OLD_PREFIX}\E/${NEW_PREFIX}/g;
                 s/\Q${OLD_MODULE}\E/${MODULE}/g;" "$FILE"
done < <(find . -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.yml' -o -name '*.yaml' -o -name '*.md' -o -name '*.properties' \) -not -path './build/*' -not -path '*/build/*' -not -path './.git/*')

# 5) Gradle group
perl -pi -e "s/\Qgroup = \"${OLD_GROUP}\"\E/group = \"${GROUP}\"/" build.gradle.kts

# 6) 패키지가 바뀌면 import 정렬이 달라진다.
#    ktlintFormat 은 import 순서를 고쳐 주지 않으므로(검사만 한다) 여기서 직접 맞춘다.
#    ktlint 기본 레이아웃: 나머지 → java.** → javax.** → kotlin.** → 별칭, 각 묶음은 사전순.
echo
echo "import 정렬을 다시 맞추는 중..."
find "$MODULE" -type f -name '*.kt' -not -path '*/build/*' -exec perl -0777 -pi -e '
    sub sort_imports {
        my @lines = split /\n/, shift;
        my (@other, @java, @javax, @kotlin, @alias);
        for my $line (@lines) {
            if    ($line =~ / as /)          { push @alias,  $line }
            elsif ($line =~ /^import java\./)  { push @java,   $line }
            elsif ($line =~ /^import javax\./) { push @javax,  $line }
            elsif ($line =~ /^import kotlin\./){ push @kotlin, $line }
            else                              { push @other,  $line }
        }
        return join("\n", (sort @other), (sort @java), (sort @javax), (sort @kotlin), (sort @alias)) . "\n";
    }
    s/((?:^import .*\n)+)/sort_imports($1)/me;
' {} +

echo
echo "완료했습니다. 이어서 확인하세요:"
echo "  LANG=C.UTF-8 ./gradlew check"
echo "  LANG=C.UTF-8 ./gradlew :${MODULE}:updateOpenApi   # 바뀐 문서 제목으로 스펙 갱신"
echo
echo "그리고 손으로 할 일:"
echo "  - GitHub 저장소 설정에서 'Template repository' 체크 (코드로는 켤 수 없다)"
echo "  - ${MODULE}/src/main/kotlin/.../config/${NEW_PREFIX}DocsConfiguration.kt 의 API 설명 수정"
echo "  - ${MODULE}/src/main/kotlin/.../user 를 지우고 실제 도메인으로 시작"
